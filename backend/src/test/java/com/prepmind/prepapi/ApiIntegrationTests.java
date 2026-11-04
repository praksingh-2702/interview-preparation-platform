package com.prepmind.prepapi;

import com.jayway.jsonpath.JsonPath;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.google.GoogleIdentity;
import com.prepmind.prepapi.google.GoogleTokenVerifier;
import com.prepmind.prepapi.mail.EmailSender;
import com.prepmind.prepapi.mail.LoggingEmailSender;
import com.prepmind.prepapi.service.AccountCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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

    @Autowired
    private AccountCleanup accountCleanup;

    private MockMvc mvc;

    /** Google stand-in: the "ID token" is just "subject|email". */
    @TestConfiguration
    static class FakeGoogle {
        @Bean
        @Primary
        GoogleTokenVerifier fakeGoogleVerifier() {
            return new GoogleTokenVerifier() {
                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public GoogleIdentity verify(String idToken) {
                    String[] parts = idToken.split("\\|");
                    if (parts.length != 2) {
                        throw ApiException.unauthorized("GOOGLE_TOKEN_INVALID", "Google sign-in failed.");
                    }
                    return new GoogleIdentity(parts[0], parts[1], true, null);
                }
            };
        }
    }

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

    /** admin_user is listed in app.admin.usernames (test properties). Logging in rotates its session. */
    private String adminToken() throws Exception {
        if (attemptLogin("admin_user", PASSWORD).andReturn().getResponse().getStatus() != 200) {
            registerVerified("admin_user");
        }
        return login("admin_user");
    }

    private long userId(String token) throws Exception {
        String json = mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions googleLogin(String subject, String email) throws Exception {
        return mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"%s|%s\"}".formatted(subject, email)));
    }

    private static String json(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
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
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.showOnLeaderboard").value(true))
                .andExpect(jsonPath("$.needsUsername").value(false));
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

        mvc.perform(get("/api/questions").param("section", "DSA").param("difficulty", "hard")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)));
    }

    @Test
    void theorySectionsLinkOutAndCanBeTracked() throws Exception {
        String token = newUserToken();
        String json = mvc.perform(get("/api/questions").param("section", "os").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(30))))
                .andExpect(jsonPath("$[0].section").value("OS"))
                .andExpect(jsonPath("$[0].problemUrl").value(org.hamcrest.Matchers.startsWith("https://www.geeksforgeeks.org/")))
                .andReturn().getResponse().getContentAsString();
        Number osId = JsonPath.read(json, "$[0].id");

        // DSA questions carry their section too
        mvc.perform(get("/api/questions").param("section", "DSA").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(100))))
                .andExpect(jsonPath("$[0].section").value("DSA"));

        // progress, revision and stats work the same for theory
        mvc.perform(put("/api/progress/" + osId).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":true,\"revised\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStage").value(0));

        mvc.perform(get("/api/questions").param("section", "PHYSICS").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_SECTION"));
    }

    @Test
    void onlyEditorsAndAdminsCanCreateQuestions() throws Exception {
        String body = """
                {"title":"Valid Anagram","category":"Arrays","difficulty":"EASY",
                 "problemUrl":"https://leetcode.com/problems/valid-anagram/","companies":["Amazon"],
                 "description":"Check if two strings are anagrams."}
                """;

        mvc.perform(post("/api/questions").header("Authorization", bearer(newUserToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        String admin = adminToken();
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
                .andExpect(jsonPath("$.activity[0].count").value(1)) // the review itself doesn't count
                .andExpect(jsonPath("$.dueForRevision").value(0))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"));

        // a theory tick lands on the same combined heatmap
        String os = mvc.perform(get("/api/questions").param("section", "OS").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        long osId = ((Number) JsonPath.read(os, "$[0].id")).longValue();
        markSolved(token, osId);
        mvc.perform(get("/api/stats").param("tz", "Asia/Kolkata").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.activity[0].count").value(2));

        // unticking removes the activity again
        for (long id : new long[] {qid, osId}) {
            mvc.perform(put("/api/progress/" + id).header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":false,\"revised\":false}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/stats").param("tz", "Asia/Kolkata").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.activity", hasSize(0)))
                .andExpect(jsonPath("$.currentStreak").value(0));

        // a bogus timezone falls back to UTC instead of erroring
        mvc.perform(get("/api/stats").param("tz", "Mars/Olympus").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Z"));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------- v3: roles + admin panel ----------

    @Test
    void adminsManageRolesAndEditorsManageQuestions() throws Exception {
        String admin = adminToken();
        String name = uniqueName();
        registerVerified(name);
        String user = login(name);
        long id = userId(user);

        mvc.perform(get("/api/admin/overview").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/overview").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.questionsBySection.DSA").value(greaterThanOrEqualTo(100)))
                .andExpect(jsonPath("$.usersByRole.ADMIN").value(greaterThanOrEqualTo(1)));
        mvc.perform(get("/api/admin/users").param("q", name).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].verified").value(true));

        // promote to EDITOR: applies to the user's existing token right away
        mvc.perform(put("/api/admin/users/" + id + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"EDITOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));
        mvc.perform(get("/api/users/me").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.role").value("EDITOR"));

        String body = """
                {"section":"CN","title":"Editor topic %s","category":"Basics","difficulty":"EASY",
                 "problemUrl":"https://www.geeksforgeeks.org/computer-networks/editor-%s/"}
                """.formatted(name, name);
        String created = json(mvc.perform(post("/api/questions").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.section").value("CN")));
        long qid = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(put("/api/questions/" + qid).header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("EASY", "HARD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("HARD"));

        // editors can't manage users
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());

        // deleting also clears everyone's progress on it
        mvc.perform(put("/api/progress/" + qid).header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":true,\"revised\":false}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/questions/" + qid).header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/progress/my-status").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$['" + qid + "']").doesNotExist());

        // demote back to USER: writes are refused again
        mvc.perform(put("/api/admin/users/" + id + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/questions/" + firstQuestionId(user)).header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());

        // an admin can't change their own role (so there's always an admin left)
        mvc.perform(put("/api/admin/users/" + userId(admin) + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_CHANGE_OWN_ROLE"));
    }

    // ---------- v3: usernames ----------

    @Test
    void usernamesAreUniqueAndCanBeChanged() throws Exception {
        String taken = uniqueName();
        registerVerified(taken);
        String name = uniqueName();
        registerVerified(name);
        String token = login(name);

        mvc.perform(get("/api/users/username-available").param("username", taken.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
        mvc.perform(get("/api/users/username-available").param("username", "a b"))
                .andExpect(jsonPath("$.available").value(false));
        // your own current name counts as available to you
        mvc.perform(get("/api/users/username-available").param("username", name).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.available").value(true));

        mvc.perform(put("/api/users/me/username").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"%s\"}".formatted(taken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));

        String renamed = name + "_v2";
        String fresh = JsonPath.read(json(mvc.perform(put("/api/users/me/username").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"%s\"}".formatted(renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(renamed))), "$.token");

        // both the old token (same session) and the new one keep working
        for (String t : List.of(token, fresh)) {
            mvc.perform(get("/api/users/me").header("Authorization", bearer(t)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(renamed));
        }
        attemptLogin(renamed, PASSWORD).andExpect(status().isOk());
    }

    // ---------- v3: leaderboard ----------

    private long questionWithDifficulty(String token, String section, String difficulty) throws Exception {
        String list = json(mvc.perform(get("/api/questions").param("section", section).param("difficulty", difficulty)
                .header("Authorization", bearer(token))));
        return ((Number) JsonPath.read(list, "$[0].id")).longValue();
    }

    @Test
    void leaderboardRanksByPointsAndRespectsOptOut() throws Exception {
        String a = uniqueName();
        String b = uniqueName();
        registerVerified(a);
        registerVerified(b);
        String ta = login(a);
        String tb = login(b);

        // A: one hard OS topic (3) + one easy DSA problem (1) = 4. B: one medium CN topic = 2.
        markSolved(ta, questionWithDifficulty(ta, "OS", "HARD"));
        markSolved(ta, questionWithDifficulty(ta, "DSA", "EASY"));
        markSolved(tb, questionWithDifficulty(tb, "CN", "MEDIUM"));

        String board = json(mvc.perform(get("/api/leaderboard").param("tz", "Asia/Kolkata").param("limit", "100")
                        .header("Authorization", bearer(ta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.username").value(a))
                .andExpect(jsonPath("$.me.points").value(4))
                .andExpect(jsonPath("$.me.completed").value(2))
                .andExpect(jsonPath("$.me.currentStreak").value(1))
                .andExpect(jsonPath("$.me.you").value(true)));
        List<Integer> rankA = JsonPath.read(board, "$.entries[?(@.username == '" + a + "')].rank");
        List<Integer> rankB = JsonPath.read(board, "$.entries[?(@.username == '" + b + "')].rank");
        org.junit.jupiter.api.Assertions.assertTrue(rankA.get(0) < rankB.get(0), "4 points should outrank 2");

        // B opts out: gone from the list, still sees their own points
        mvc.perform(put("/api/users/me/preferences").header("Authorization", bearer(tb))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"showOnLeaderboard\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showOnLeaderboard").value(false));
        mvc.perform(get("/api/leaderboard").param("limit", "100").header("Authorization", bearer(tb)))
                .andExpect(jsonPath("$.entries[?(@.username == '" + b + "')]", empty()))
                .andExpect(jsonPath("$.me.rank").value(nullValue()))
                .andExpect(jsonPath("$.me.listed").value(false))
                .andExpect(jsonPath("$.me.points").value(2));
    }

    // ---------- v3: Google sign-in ----------

    @Test
    void googleSignInCreatesAccountThatPicksAUsername() throws Exception {
        String sub = "g-" + uniqueName();
        String email = sub + "@gmail.com";
        String first = json(googleLogin(sub, email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsUsername").value(true))
                .andExpect(jsonPath("$.username").value(org.hamcrest.Matchers.startsWith("google-"))));
        String token = JsonPath.read(first, "$.token");

        // placeholder accounts aren't ranked
        mvc.perform(get("/api/leaderboard").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.me.rank").value(nullValue()));

        // placeholder-style names can't be picked
        mvc.perform(put("/api/users/me/username").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"google-123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USERNAME_INVALID"));

        String chosen = uniqueName();
        mvc.perform(put("/api/users/me/username").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"%s\"}".formatted(chosen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsUsername").value(false));

        // signing in again: same account, no picker
        String again = json(googleLogin(sub, email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(chosen))
                .andExpect(jsonPath("$.needsUsername").value(false)));
        mvc.perform(get("/api/users/me").header("Authorization", bearer(JsonPath.read(again, "$.token"))))
                .andExpect(jsonPath("$.googleLinked").value(true));
    }

    @Test
    void googleSignInLinksToAnExistingVerifiedAccount() throws Exception {
        String name = uniqueName();
        registerVerified(name);
        String token = login(name);
        long qid = firstQuestionId(token);
        markSolved(token, qid);

        String json = json(googleLogin("g-" + name, emailOf(name).toUpperCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.needsUsername").value(false)));
        mvc.perform(get("/api/progress/my-status").header("Authorization", bearer(JsonPath.read(json, "$.token"))))
                .andExpect(jsonPath("$['" + qid + "'].solved").value(true));
        // the password still works too
        attemptLogin(name, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void googleSignInTakesOverAnUnverifiedSignUpSafely() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated()); // never verified

        googleLogin("g-" + name, emailOf(name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsUsername").value(true));
        // whoever started that sign-up never proved they own the inbox: their password no longer works
        attemptLogin(name, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void googleRejectsBadTokens() throws Exception {
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"nonsense\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GOOGLE_TOKEN_INVALID"));
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- v3: deleting accounts ----------

    @Test
    void adminsCanDeleteUsersButNotThemselves() throws Exception {
        String admin = adminToken();
        String name = uniqueName();
        registerVerified(name);
        String token = login(name);
        markSolved(token, firstQuestionId(token));
        long id = userId(token);

        mvc.perform(delete("/api/admin/users/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/users/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        // the account and its sessions are gone
        mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        attemptLogin(name, PASSWORD).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").param("q", name).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(delete("/api/admin/users/" + userId(admin)).header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DELETE_SELF"));
    }

    @Test
    void abandonedSignUpsAreRemovedButFreshOnesAndVerifiedAccountsStay() throws Exception {
        String admin = adminToken();
        String verified = uniqueName();
        registerVerified(verified);
        String pending = uniqueName();
        register(pending, PASSWORD).andExpect(status().isCreated());

        // the admin button keeps sign-ups from the last hour (someone may be typing the code)
        mvc.perform(post("/api/admin/users/remove-unverified").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removed").exists());
        mvc.perform(get("/api/admin/users").param("q", pending).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$", hasSize(1)));

        // once they're old enough they go (negative age = "created before a minute from now")
        accountCleanup.removeUnverifiedOlderThan(Duration.ofMinutes(-1));
        mvc.perform(get("/api/admin/users").param("q", pending).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/admin/users").param("q", verified).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$", hasSize(1)));

        // the email is free to sign up again
        register(pending, PASSWORD).andExpect(status().isCreated());
    }
}
