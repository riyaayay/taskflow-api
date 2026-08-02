package com.riya.taskflow.service;

import com.riya.taskflow.dto.TaskRequest;
import com.riya.taskflow.dto.TaskResponse;
import com.riya.taskflow.exception.ResourceNotFoundException;
import com.riya.taskflow.model.Task;
import com.riya.taskflow.model.TaskStatus;
import com.riya.taskflow.model.User;
import com.riya.taskflow.repository.TaskRepository;
import com.riya.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(String ownerEmail, TaskRequest request) {
        User owner = getUserByEmail(ownerEmail);

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : com.riya.taskflow.model.TaskStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : com.riya.taskflow.model.TaskPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .owner(owner)
                .build();

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public Page<TaskResponse> listTasks(String ownerEmail, TaskStatus status, Pageable pageable) {
        User owner = getUserByEmail(ownerEmail);

        Page<Task> tasks = status != null
                ? taskRepository.findByOwnerIdAndStatus(owner.getId(), status, pageable)
                : taskRepository.findByOwnerId(owner.getId(), pageable);

        return tasks.map(TaskResponse::fromEntity);
    }

    public TaskResponse getTask(String ownerEmail, Long taskId) {
        Task task = getOwnedTask(ownerEmail, taskId);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse updateTask(String ownerEmail, Long taskId, TaskRequest request) {
        Task task = getOwnedTask(ownerEmail, taskId);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(String ownerEmail, Long taskId) {
        Task task = getOwnedTask(ownerEmail, taskId);
        taskRepository.delete(task);
    }

    private Task getOwnedTask(String ownerEmail, Long taskId) {
        User owner = getUserByEmail(ownerEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        if (!task.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
        return task;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
