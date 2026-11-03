package com.prepmind.prepapi;

import com.jayway.jsonpath.JsonPath;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
        register(name, PASSWORD).andExpect(status().isCreated());
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
        register(name, PASSWORD).andExpect(status().isCreated());
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
        register(name, PASSWORD).andExpect(status().isCreated());
        register(name.toUpperCase(), PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    void wrongPasswordIs401AndRepeatedFailuresAreRateLimited() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());

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
        register(name, PASSWORD).andExpect(status().isCreated());
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
        register(name, PASSWORD).andExpect(status().isCreated());
        mvc.perform(get("/api/users/me").header("Authorization", bearer(login(name))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.email").value(name.toLowerCase() + "@example.com"))
                .andExpect(jsonPath("$.roles", not(hasItem("ROLE_ADMIN"))));
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

        register("admin_user", PASSWORD).andExpect(status().isCreated());
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

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
