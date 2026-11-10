package com.twitter.ums;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.ums.contracts.LoginResponse;
import com.twitter.ums.contracts.UserCreatedResponse;
import com.twitter.ums.web.ApiCorsFilter;
import com.twitter.ums.web.UserController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registersUserWithGuestRole() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", "2000-05-01");

        mockMvc.perform(get("/api/users/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login))
                .andExpect(jsonPath("$.email").value(login + "@example.com"))
                .andExpect(jsonPath("$.name").value("Кристина"))
                .andExpect(jsonPath("$.birthDate").value("2000-05-01"))
                .andExpect(jsonPath("$.roles[0].code").value("GUEST"))
                .andExpect(jsonPath("$.lastSession").value(nullValue()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsDuplicateLoginAndEmail() throws Exception {
        String login = uniqueLogin();
        register(login, login + "@example.com", "Кристина", null);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(login, "other-" + login + "@example.com", "Кристина", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("login already exists"));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueLogin(), login + "@example.com", "Кристина", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("email already exists"));
    }

    @Test
    void rejectsInvalidRegistration() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("ab", "bad", "Кристина", "2999-01-01")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"valid_user","password":"short","email":"valid@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password must be 8-72 characters"));
    }

    @Test
    void logsInAndRejectsBadPassword() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", null);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"wrong-password"}
                                """.formatted(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("invalid credentials"));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"missing-user","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("invalid credentials"));

        LoginResponse session = login(login);
        org.junit.jupiter.api.Assertions.assertEquals(created.id(), session.userId());

        mockMvc.perform(get("/api/users/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastSession.id").value(session.sessionId()))
                .andExpect(jsonPath("$.lastSession.active").value(true));
    }

    @Test
    void listsSeededRoles() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("ADMIN")))
                .andExpect(jsonPath("$[*].code", hasItem("PRODUCER")))
                .andExpect(jsonPath("$[*].code", hasItem("SUBSCRIBER")))
                .andExpect(jsonPath("$[*].code", hasItem("GUEST")));
    }

    @Test
    void listsUsersWithRoles() throws Exception {
        String login = uniqueLogin();
        register(login, login + "@example.com", "Кристина", null);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].login", hasItem("admin")))
                .andExpect(jsonPath("$[*].login", hasItem(login)))
                .andExpect(jsonPath("$[?(@.login == 'admin')].roles[*].code", hasItem("ADMIN")));
    }

    @Test
    void returnsNotFoundForMissingUser() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("user not found"));
    }

    @Test
    void updatesUserAndResyncsRoles() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", "2000-05-01");
        String renamed = login + "_new";

        mockMvc.perform(put("/api/users/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login":"%s",
                                  "email":"%s@example.com",
                                  "name":"Новое имя",
                                  "birthDate":"1999-01-02",
                                  "roles":["ADMIN","PRODUCER"]
                                }
                                """.formatted(renamed, renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(renamed))
                .andExpect(jsonPath("$.name").value("Новое имя"))
                .andExpect(jsonPath("$.roles[*].code", hasItem("ADMIN")))
                .andExpect(jsonPath("$.roles[*].code", hasItem("PRODUCER")))
                .andExpect(jsonPath("$.roles[*].code", not(hasItem("GUEST"))));
    }

    @Test
    void unknownRoleDoesNotChangeUser() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", null);

        mockMvc.perform(put("/api/users/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login":"%s",
                                  "email":"changed-%s@example.com",
                                  "roles":["SUBSCRIBER","UNKNOWN"]
                                }
                                """.formatted(login, login)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("unknown role"));

        mockMvc.perform(get("/api/users/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(login + "@example.com"))
                .andExpect(jsonPath("$.roles[0].code").value("GUEST"));
    }

    @Test
    void patchesEmailAndName() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", null);

        mockMvc.perform(patch("/api/users/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Только имя"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Только имя"))
                .andExpect(jsonPath("$.email").value(login + "@example.com"));

        mockMvc.perform(patch("/api/users/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"fresh-%s@example.com"}
                                """.formatted(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("fresh-" + login + "@example.com"))
                .andExpect(jsonPath("$.name").value("Только имя"));
    }

    @Test
    void rejectsPatchConflictsAndEmptyBody() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", null);

        mockMvc.perform(patch("/api/users/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("at least one of email or name is required"));

        mockMvc.perform(patch("/api/users/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@twitter.local"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("email already exists"));

        mockMvc.perform(patch("/api/users/{id}", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Никто"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("user not found"));
    }

    @Test
    void deletesUserAndCascadesRolesAndSessions() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", null);
        LoginResponse session = login(login);

        mockMvc.perform(delete("/api/users/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("deleted"));

        mockMvc.perform(get("/api/users/{id}", created.id()))
                .andExpect(status().isNotFound());

        Integer roles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ?",
                Integer.class,
                created.id()
        );
        Integer sessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE id = ?",
                Integer.class,
                session.sessionId()
        );
        org.junit.jupiter.api.Assertions.assertEquals(0, roles);
        org.junit.jupiter.api.Assertions.assertEquals(0, sessions);

        mockMvc.perform(delete("/api/sessions/{id}", session.sessionId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("session not found"));
    }

    @Test
    void assignsRoleOnlyWhenCallerIsAdmin() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", null);
        LoginResponse userSession = login(login);
        LoginResponse adminSession = login("admin");

        mockMvc.perform(post("/api/users/{id}/roles", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"PRODUCER"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("у вызывающего нет роли ADMIN"));

        mockMvc.perform(post("/api/users/{id}/roles", created.id())
                        .header(UserController.SESSION_HEADER, userSession.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"PRODUCER"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users/{id}/roles", created.id())
                        .header(UserController.SESSION_HEADER, adminSession.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"producer"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PRODUCER"))
                .andExpect(jsonPath("$.message").value("role assigned"));

        mockMvc.perform(post("/api/users/{id}/roles", created.id())
                        .header(UserController.SESSION_HEADER, adminSession.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"PRODUCER"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("role already assigned"));

        mockMvc.perform(post("/api/users/{id}/roles", created.id())
                        .header(UserController.SESSION_HEADER, adminSession.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"MODERATOR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("unknown role"));

        mockMvc.perform(get("/api/users/{id}", created.id()))
                .andExpect(jsonPath("$.roles[*].code", hasItem("GUEST")))
                .andExpect(jsonPath("$.roles[*].code", hasItem("PRODUCER")));
    }

    @Test
    void returnsLastSessionAndClosesIt() throws Exception {
        String login = uniqueLogin();
        UserCreatedResponse created = register(login, login + "@example.com", "Кристина", null);

        mockMvc.perform(get("/api/users/{id}/sessions/last", created.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("session not found"));

        LoginResponse first = login(login);
        LoginResponse second = login(login);

        mockMvc.perform(get("/api/users/{id}/sessions/last", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second.sessionId()))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(delete("/api/sessions/{id}", second.sessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("session closed"));

        mockMvc.perform(get("/api/users/{id}/sessions/last", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second.sessionId()))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/sessions/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("некорректный идентификатор"));

        org.junit.jupiter.api.Assertions.assertNotEquals(first.sessionId(), second.sessionId());
    }

    @Test
    void rejectsForeignOriginAndUnknownMethod() throws Exception {
        mockMvc.perform(get("/api/roles").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value(ApiCorsFilter.MESSAGE));

        mockMvc.perform(get("/api/roles").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mockMvc.perform(options("/api/roles")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(request(HttpMethod.TRACE, "/api/roles"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ApiCorsFilter.MESSAGE));
    }

    private UserCreatedResponse register(String login, String email, String name, String birthDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(login, email, name, birthDate)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/users/")))
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), UserCreatedResponse.class);
    }

    private LoginResponse login(String login) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"%s"}
                                """.formatted(login, "admin".equals(login) ? "admin12345" : "password123")))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
    }

    private static String registerBody(String login, String email, String name, String birthDate) {
        String birth = birthDate == null ? "null" : "\"" + birthDate + "\"";
        return """
                {"login":"%s","password":"password123","email":"%s","name":"%s","birthDate":%s}
                """.formatted(login, email, name, birth);
    }

    private static String uniqueLogin() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
