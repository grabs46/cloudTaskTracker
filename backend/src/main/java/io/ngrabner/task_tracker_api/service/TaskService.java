package io.ngrabner.task_tracker_api.service;

import io.ngrabner.task_tracker_api.domain.Task;
import io.ngrabner.task_tracker_api.repository.TaskRepository;
import io.ngrabner.task_tracker_api.web.dto.PagedResponse;
import io.ngrabner.task_tracker_api.web.dto.task.CreateTaskRequest;
import io.ngrabner.task_tracker_api.web.dto.task.TaskResponse;
import io.ngrabner.task_tracker_api.web.dto.task.TaskStatus;
import io.ngrabner.task_tracker_api.web.dto.task.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse createTask(Long userId, CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setUserId(userId);
        TaskStatus status = request.getStatus() != null ? request.getStatus() : TaskStatus.TODO;
        task.setStatus(status.name());

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    public TaskResponse getTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        return toResponse(task);
    }

    public List<TaskResponse> listTasks(Long userId) {
        return taskRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "title");

    private static final int MAX_PAGE_SIZE = 100;

    public PagedResponse<TaskResponse> searchTasks(
            Long userId,
            String query,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;

        // Validate and sanitize sort field
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "createdAt";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // Normalize empty strings to null for the query
        String normalizedQuery = (query != null && query.isBlank()) ? null : query;
        String normalizedStatus = (status != null && status.isBlank()) ? null : status;

        Page<Task> taskPage = taskRepository.searchTasks(userId, normalizedQuery, normalizedStatus, pageable);

        List<TaskResponse> content = taskPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages()
        );
    }

    public TaskResponse updateTask(Long userId, Long taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus().name());
        }

        Task updatedTask = taskRepository.save(task);
        return toResponse(updatedTask);
    }

    public void deleteTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        taskRepository.delete(task);
    }

    private TaskResponse toResponse(Task task) {
        TaskStatus status = TaskStatus.valueOf(task.getStatus());

        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(status);
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setPriority(task.getPriority());
        response.setDueAt(task.getDueAt());
        return response;
    }
}