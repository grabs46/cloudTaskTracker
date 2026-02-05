package io.ngrabner.task_tracker_api.web.dto.task;

import jakarta.validation.constraints.Size;

public class UpdateTaskRequest {
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    private TaskStatus status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
