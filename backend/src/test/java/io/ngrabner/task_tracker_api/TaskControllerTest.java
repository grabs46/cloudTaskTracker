package io.ngrabner.task_tracker_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ngrabner.task_tracker_api.auth.CurrentUser;
import io.ngrabner.task_tracker_api.domain.User;
import io.ngrabner.task_tracker_api.repository.TaskRepository;
import io.ngrabner.task_tracker_api.repository.UserRepository;
import io.ngrabner.task_tracker_api.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private TaskRepository taskRepository;

    @Value("${app.jwt.cookie-name:tt_access}")
    private String cookieName;

    private Cookie authCookie;
    private Long userId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setGoogleSub("google-test-sub");
        user.setEmail("testuser@example.com");
        user.setName("Test User");
        user = userRepository.save(user);
        userId = user.getId();

        String token = jwtService.createToken(userId, user.getEmail());
        authCookie = new Cookie(cookieName, token);
    }

    // --- Authentication ---

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    // --- Create ---

    @Test
    void createTask_returns201_withValidInput() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Write tests",
                "description", "Integration tests for Day 5"));

        mockMvc.perform(post("/api/tasks")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.description").value("Integration tests for Day 5"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createTask_returns400_whenTitleIsBlank() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", ""));

        mockMvc.perform(post("/api/tasks")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createTask_returns400_whenTitleIsMissing() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("description", "No title"));

        mockMvc.perform(post("/api/tasks")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- Read ---

    @Test
    void getTaskById_returnsTask() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Fetch me"));

        String response = mockMvc.perform(post("/api/tasks")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fetch me"));
    }

    @Test
    void getTaskById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/99999")
                        .cookie(authCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void getAllTasks_returnsPaginatedResponse() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/tasks")
                    .cookie(authCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("title", "Task " + i))));
        }

        mockMvc.perform(get("/api/tasks")
                        .cookie(authCookie)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // --- Update ---

    @Test
    void updateTask_changesFields() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Original"));

        String response = mockMvc.perform(post("/api/tasks")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(response).get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Updated",
                "status", "IN_PROGRESS"));

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateTask_returns404_whenNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "Nope"));

        mockMvc.perform(put("/api/tasks/99999")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- Delete ---

    @Test
    void deleteTask_returns204() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Delete me"));

        String response = mockMvc.perform(post("/api/tasks")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .cookie(authCookie))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .cookie(authCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_returns404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/api/tasks/99999")
                        .cookie(authCookie))
                .andExpect(status().isNotFound());
    }

    // --- Ownership isolation ---

    @Test
    void otherUser_cannotAccessTask() throws Exception {
        // Create a task as the main user
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Private Task"));

        String response = mockMvc.perform(post("/api/tasks")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(response).get("id").asLong();

        // Create a second user and their auth cookie
        User other = new User();
        other.setGoogleSub("google-other-sub");
        other.setEmail("other@example.com");
        other.setName("Other User");
        other = userRepository.save(other);
        Cookie otherCookie = new Cookie(cookieName, jwtService.createToken(other.getId(), other.getEmail()));

        // Other user should not see the task
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound());
    }

    // --- Search ---

    @Test
    void searchTasks_filtersByQueryParam() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Buy groceries"))));
        mockMvc.perform(post("/api/tasks")
                .cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Read a book"))));

        mockMvc.perform(get("/api/tasks")
                        .cookie(authCookie)
                        .param("query", "groceries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Buy groceries"));
    }
}
