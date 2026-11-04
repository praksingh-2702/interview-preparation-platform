package com.prepmind.prepapi;

import com.jayway.jsonpath.JsonPath;
import com.prepmind.prepapi.mail.EmailSender;
import com.prepmind.prepapi.mail.LoggingEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ApiIntegrationTests {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String PASSWORD = "correct-horse-1";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EmailSender emailSender;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---------- helpers ----------

    private static String uniqueName() {
        return "user" + SEQ.incrementAndGet() + "_" + (System.nanoTime() % 100_000);
    }

    private ResultActions register(String username, String password) throws Exception {
        String body = "{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"%s\"}"
                .formatted(username, username, password);
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String emailOf(String username) {
        return username.toLowerCase() + "@example.com";
    }

    /** Reads the 6-digit code from the last email "sent" to this address (logging sender in tests). */
    private String lastCode(String email) {
        String message = ((LoggingEmailSender) emailSender).lastMessageTo(email);
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(message == null ? "" : message);
        if (!m.find()) {
            throw new AssertionError("No code emailed to " + email);
        }
        return m.group(1);
    }

    private ResultActions verify(String email, String code) throws Exception {
        return mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code)));
    }

    private void registerVerified(String username) throws Exception {
        register(username, PASSWORD).andExpect(status().isCreated());
        verify(emailOf(username), lastCode(emailOf(username))).andExpect(status().isOk());
    }

    private ResultActions attemptLogin(String username, String password) throws Exception {
        String body = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String login(String username) throws Exception {
        String json = attemptLogin(username, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String newUserToken() throws Exception {
        String name = uniqueName();
        registerVerified(name);
        return login(name);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private long firstQuestionId(String token) throws Exception {
        String json = mvc.perform(get("/api/questions").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(json, "$[0].id");
        return id.longValue();
    }

    // ---------- auth ----------

    @Test
    void protectedEndpointsReturnJson401WithoutToken() throws Exception {
        mvc.perform(get("/api/questions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void garbageTokenIsRejectedButLoginStillWorks() throws Exception {
        mvc.perform(get("/api/questions").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        String name = uniqueName();
        registerVerified(name);
        String body = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(name, PASSWORD);
        mvc.perform(post("/api/auth/login").header("Authorization", "Bearer stale")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void registrationValidatesInput() throws Exception {
        register("ab", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void duplicateUsernameIsConflict() throws Exception {
        String name = uniqueName();
        registerVerified(name);
        String body = "{\"username\":\"%s\",\"email\":\"other_%s@example.com\",\"password\":\"%s\"}"
                .formatted(name.toUpperCase(), name, PASSWORD);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
        register(name, PASSWORD) // same email, already verified
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void wrongPasswordIs401AndRepeatedFailuresAreRateLimited() throws Exception {
        String name = uniqueName();
        registerVerified(name);

        for (int i = 0; i < 3; i++) { // app.auth.max-failed-logins=3 in the test profile
            attemptLogin(name, "wrong-password").andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        attemptLogin(name, PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
    }

    @Test
    void newLoginRevokesTheOlderToken() throws Exception {
        String name = uniqueName();
        registerVerified(name);
        String first = login(name);
        String second = login(name);

        mvc.perform(get("/api/questions").header("Authorization", bearer(first)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
        mvc.perform(get("/api/questions").header("Authorization", bearer(second)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutInvalidatesTokenOnTheServer() throws Exception {
        String token = newUserToken();
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsProfileWithoutAdminRole() throws Exception {
        String name = uniqueName();
        registerVerified(name);
        mvc.perform(get("/api/users/me").header("Authorization", bearer(login(name))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.email").value(name.toLowerCase() + "@example.com"))
                .andExpect(jsonPath("$.roles", not(hasItem("ROLE_ADMIN"))));
    }

    // ---------- v2: email verification + password reset ----------

    @Test
    void unverifiedAccountsCannotLogInUntilTheCodeIsEntered() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(emailOf(name)));

        attemptLogin(name, PASSWORD).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.email").value(emailOf(name)));

        verify(emailOf(name), "000000".equals(lastCode(emailOf(name))) ? "111111" : "000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_INVALID"));
        verify(emailOf(name), lastCode(emailOf(name))).andExpect(status().isOk());
        verify(emailOf(name), "123456").andExpect(status().isOk()); // already verified: idempotent

        attemptLogin(name, PASSWORD).andExpect(status().isOk());
        attemptLogin(emailOf(name).toUpperCase(), PASSWORD).andExpect(status().isOk()); // login by email
    }

    @Test
    void codesAreRateLimitedAndLockAfterTooManyWrongGuesses() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailOf(name) + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));

        String real = lastCode(emailOf(name));
        String wrong = real.equals("999999") ? "888888" : "999999";
        for (int i = 0; i < 4; i++) {
            verify(emailOf(name), wrong).andExpect(jsonPath("$.code").value("OTP_INVALID"));
        }
        verify(emailOf(name), wrong).andExpect(jsonPath("$.code").value("OTP_EXPIRED"));
        verify(emailOf(name), real).andExpect(status().isBadRequest()); // burned after 5 misses
    }

    @Test
    void abandonedSignUpCanBeRedoneWithTheSameEmail() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());
        String body = "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
                .formatted(name + "x", emailOf(name), "another-pass-1");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        verify(emailOf(name), lastCode(emailOf(name))).andExpect(status().isOk());
        attemptLogin(name + "x", "another-pass-1").andExpect(status().isOk());
    }

    @Test
    void forgotPasswordResetsAndSignsOutEverywhere() throws Exception {
        String name = uniqueName();
        registerVerified(name);
        String oldToken = login(name);

        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailOf(name) + "\"}"))
                .andExpect(status().isOk());
        // unknown emails get the same answer (no account probing)
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody_here@example.com\"}"))
                .andExpect(status().isOk());

        String reset = "{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"brand-new-pass\"}"
                .formatted(emailOf(name), lastCode(emailOf(name)));
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(reset))
                .andExpect(status().isOk());

        mvc.perform(get("/api/users/me").header("Authorization", bearer(oldToken)))
                .andExpect(status().isUnauthorized());
        attemptLogin(name, PASSWORD).andExpect(status().isUnauthorized());
        attemptLogin(name, "brand-new-pass").andExpect(status().isOk());
    }

    // ---------- questions ----------

    @Test
    void questionsAreSeededAndFilterable() throws Exception {
        String token = newUserToken();
        mvc.perform(get("/api/questions").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(100))))  // admin test may add one
                .andExpect(jsonPath("$[0].title").value("Two Sum"))
                .andExpect(jsonPath("$[0].companies", hasItem("Google")));

        mvc.perform(get("/api/questions").param("difficulty", "hard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)));
    }

    @Test
    void onlyAdminsCanCreateQuestions() throws Exception {
        String body = """
                {"title":"Valid Anagram","category":"Arrays","difficulty":"EASY",
                 "problemUrl":"https://leetcode.com/problems/valid-anagram/","companies":["Amazon"],
                 "description":"Check if two strings are anagrams."}
                """;

        mvc.perform(post("/api/questions").header("Authorization", bearer(newUserToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        registerVerified("admin_user");
        String admin = login("admin_user");
        mvc.perform(post("/api/questions").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
        mvc.perform(post("/api/questions").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // ---------- progress ----------

    @Test
    void progressRoundTrip() throws Exception {
        String token = newUserToken();
        long qid = firstQuestionId(token);

        mvc.perform(put("/api/progress/" + qid).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":true,\"revised\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true));

        mvc.perform(get("/api/progress/my-status").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['" + qid + "'].solved").value(true))
                .andExpect(jsonPath("$['" + qid + "'].revised").value(false));

        // legacy endpoint still works
        mvc.perform(post("/api/progress/toggle").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + qid + ",\"solved\":true,\"revised\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/progress/my-status").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$['" + qid + "'].revised").value(true));

        // clearing both flags removes the record
        mvc.perform(put("/api/progress/" + qid).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":false,\"revised\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/progress/my-status").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$['" + qid + "']").doesNotExist());
    }

    @Test
    void progressForUnknownQuestionIs404() throws Exception {
        mvc.perform(put("/api/progress/999999").header("Authorization", bearer(newUserToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":true,\"revised\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void progressIsPrivatePerUser() throws Exception {
        String alice = newUserToken();
        String bob = newUserToken();
        long qid = firstQuestionId(alice);

        mvc.perform(put("/api/progress/" + qid).header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":true,\"revised\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/progress/my-status").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$['" + qid + "']").doesNotExist());
    }

    // ---------- v2: revision queue ----------

    private void markSolved(String token, long qid) throws Exception {
        mvc.perform(put("/api/progress/" + qid).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":true,\"revised\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStage").value(0))
                .andExpect(jsonPath("$.nextReviewAt").exists())
                .andExpect(jsonPath("$.solvedAt").exists());
    }

    private ResultActions review(String token, long qid, String result) throws Exception {
        return mvc.perform(post("/api/progress/" + qid + "/review").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"result\":\"" + result + "\"}"));
    }

    @Test
    void revisionQueueSchedulesAndResets() throws Exception {
        String token = newUserToken();
        long qid = firstQuestionId(token);
        markSolved(token, qid);

        review(token, qid, "GOOD").andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStage").value(1))
                .andExpect(jsonPath("$.revised").value(true));
        review(token, qid, "GOOD").andExpect(jsonPath("$.reviewStage").value(2));
        review(token, qid, "AGAIN").andExpect(jsonPath("$.reviewStage").value(0));

        mvc.perform(get("/api/progress/my-status").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$['" + qid + "'].nextReviewAt").exists())
                .andExpect(jsonPath("$['" + qid + "'].reviewStage").value(0));

        // un-solving clears the schedule
        mvc.perform(put("/api/progress/" + qid).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":false,\"revised\":true}"))
                .andExpect(jsonPath("$.nextReviewAt").value(nullValue()));
    }

    @Test
    void reviewingAnUnsolvedProblemIsRejected() throws Exception {
        String token = newUserToken();
        review(token, firstQuestionId(token), "GOOD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_SOLVED"));
        review(token, firstQuestionId(token), "MAYBE").andExpect(status().isBadRequest());
    }

    // ---------- v2: notes ----------

    @Test
    void notesCanBeSavedListedAndCleared() throws Exception {
        String token = newUserToken();
        long qid = firstQuestionId(token);

        mvc.perform(put("/api/notes/" + qid).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  Hash map, O(n) time  \",\"solutionUrl\":\"https://github.com/me/two-sum\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hash map, O(n) time"));

        mvc.perform(get("/api/notes").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['" + qid + "'].solutionUrl").value("https://github.com/me/two-sum"));

        // other users can't see it
        mvc.perform(get("/api/notes").header("Authorization", bearer(newUserToken())))
                .andExpect(jsonPath("$['" + qid + "']").doesNotExist());

        mvc.perform(put("/api/notes/" + qid).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"x\",\"solutionUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.solutionUrl").exists());

        mvc.perform(put("/api/notes/" + qid).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \",\"solutionUrl\":\"\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notes").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$['" + qid + "']").doesNotExist());

        mvc.perform(delete("/api/notes/" + qid).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    // ---------- v2: stats ----------

    @Test
    void statsReportTodaysActivityAndStreak() throws Exception {
        String token = newUserToken();
        mvc.perform(get("/api/stats").param("tz", "Asia/Kolkata").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.activity", hasSize(0)));

        long qid = firstQuestionId(token);
        markSolved(token, qid);
        review(token, qid, "GOOD").andExpect(status().isOk());

        mvc.perform(get("/api/stats").param("tz", "Asia/Kolkata").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(1))
                .andExpect(jsonPath("$.activity", hasSize(1)))
                .andExpect(jsonPath("$.activity[0].count").value(2))
                .andExpect(jsonPath("$.dueForRevision").value(0))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"));

        // a bogus timezone falls back to UTC instead of erroring
        mvc.perform(get("/api/stats").param("tz", "Mars/Olympus").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Z"));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
