package io.ngrabner.task_tracker_api.web.controller;

import io.ngrabner.task_tracker_api.auth.CurrentUser;
import io.ngrabner.task_tracker_api.service.TaskService;
import io.ngrabner.task_tracker_api.web.dto.PagedResponse;
import io.ngrabner.task_tracker_api.web.dto.task.CreateTaskRequest;
import io.ngrabner.task_tracker_api.web.dto.task.TaskResponse;
import io.ngrabner.task_tracker_api.web.dto.task.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser cu)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return cu.userId();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(Authentication authentication, @Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(currentUserId(authentication), request);
    }

    @GetMapping("/{taskId}")
    public TaskResponse getTaskById(Authentication authentication, @PathVariable Long taskId) {
        return taskService.getTask(currentUserId(authentication), taskId);
    }

    @GetMapping
    public PagedResponse<TaskResponse> getAllTasks(
            Authentication authentication,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return taskService.searchTasks(
                currentUserId(authentication),
                query,
                status,
                page,
                size,
                sortBy,
                sortDir
        );
    }

    @PutMapping("/{taskId}")
    public TaskResponse updateTask(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.updateTask(currentUserId(authentication), taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(Authentication authentication, @PathVariable Long taskId) {
        taskService.deleteTask(currentUserId(authentication), taskId);
    }
}
