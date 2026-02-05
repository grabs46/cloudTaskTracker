package io.ngrabner.task_tracker_api;

import io.ngrabner.task_tracker_api.domain.Task;
import io.ngrabner.task_tracker_api.domain.User;
import io.ngrabner.task_tracker_api.repository.TaskRepository;
import io.ngrabner.task_tracker_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setGoogleSub("google-123");
        user.setEmail("test@example.com");
        user.setName("Test User");
        userId = userRepository.save(user).getId();

        User other = new User();
        other.setGoogleSub("google-456");
        other.setEmail("other@example.com");
        other.setName("Other User");
        otherUserId = userRepository.save(other).getId();
    }

    private Task createTask(Long owner, String title, String status) {
        Task task = new Task();
        task.setUserId(owner);
        task.setTitle(title);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    @Test
    void findByIdAndUserId_returnsTask_whenOwnerMatches() {
        Task saved = createTask(userId, "My Task", "TODO");

        Optional<Task> found = taskRepository.findByIdAndUserId(saved.getId(), userId);

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("My Task");
    }

    @Test
    void findByIdAndUserId_returnsEmpty_whenOwnerDoesNotMatch() {
        Task saved = createTask(userId, "My Task", "TODO");

        Optional<Task> found = taskRepository.findByIdAndUserId(saved.getId(), otherUserId);

        assertThat(found).isEmpty();
    }

    @Test
    void findAllByUserIdOrderByCreatedAtDesc_returnsOnlyUserTasks() {
        createTask(userId, "Task A", "TODO");
        createTask(userId, "Task B", "DONE");
        createTask(otherUserId, "Other Task", "TODO");

        List<Task> tasks = taskRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        assertThat(tasks).hasSize(2);
        assertThat(tasks).allMatch(t -> t.getUserId().equals(userId));
    }

    @Test
    void searchTasks_filtersByQuery() {
        createTask(userId, "Buy groceries", "TODO");
        createTask(userId, "Read a book", "TODO");

        Page<Task> result = taskRepository.searchTasks(
                userId, "groceries", null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Buy groceries");
    }

    @Test
    void searchTasks_filtersByStatus() {
        createTask(userId, "Task A", "TODO");
        createTask(userId, "Task B", "DONE");
        createTask(userId, "Task C", "TODO");

        Page<Task> result = taskRepository.searchTasks(
                userId, null, "DONE",
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Task B");
    }

    @Test
    void searchTasks_paginatesResults() {
        for (int i = 0; i < 5; i++) {
            createTask(userId, "Task " + i, "TODO");
        }

        Page<Task> page0 = taskRepository.searchTasks(
                userId, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(3);
    }

    @Test
    void searchTasks_returnsEmpty_whenNoMatch() {
        createTask(userId, "Buy groceries", "TODO");

        Page<Task> result = taskRepository.searchTasks(
                userId, "nonexistent", null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void searchTasks_doesNotReturnOtherUsersTasks() {
        createTask(userId, "My Task", "TODO");
        createTask(otherUserId, "Their Task", "TODO");

        Page<Task> result = taskRepository.searchTasks(
                userId, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("My Task");
    }

    @Test
    void deleteByIdAndUserId_removesTask_whenOwnerMatches() {
        Task saved = createTask(userId, "To Delete", "TODO");

        taskRepository.deleteByIdAndUserId(saved.getId(), userId);
        taskRepository.flush();

        assertThat(taskRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteByIdAndUserId_doesNothing_whenOwnerDoesNotMatch() {
        Task saved = createTask(userId, "Protected", "TODO");

        taskRepository.deleteByIdAndUserId(saved.getId(), otherUserId);
        taskRepository.flush();

        assertThat(taskRepository.findById(saved.getId())).isPresent();
    }
}
