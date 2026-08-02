package com.riya.taskflow.controller;

import com.riya.taskflow.dto.TaskRequest;
import com.riya.taskflow.dto.TaskResponse;
import com.riya.taskflow.model.TaskStatus;
import com.riya.taskflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal UserDetails principal,
                                                     @Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.createTask(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> listTasks(@AuthenticationPrincipal UserDetails principal,
                                                          @RequestParam(required = false) TaskStatus status,
                                                          Pageable pageable) {
        return ResponseEntity.ok(taskService.listTasks(principal.getUsername(), status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@AuthenticationPrincipal UserDetails principal,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTask(principal.getUsername(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@AuthenticationPrincipal UserDetails principal,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal UserDetails principal,
                                            @PathVariable Long id) {
        taskService.deleteTask(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
