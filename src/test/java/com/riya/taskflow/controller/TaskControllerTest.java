package com.riya.taskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riya.taskflow.config.JwtAuthFilter;
import com.riya.taskflow.dto.TaskRequest;
import com.riya.taskflow.dto.TaskResponse;
import com.riya.taskflow.model.TaskPriority;
import com.riya.taskflow.model.TaskStatus;
import com.riya.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TaskController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class))
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    @WithMockUser(username = "riya@example.com")
    void createTask_returns201WithCreatedTask() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Design MFE shell");
        request.setDescription("Set up module federation host app");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(3));

        TaskResponse response = TaskResponse.builder()
                .id(1L)
                .title("Design MFE shell")
                .description("Set up module federation host app")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(request.getDueDate())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(taskService.createTask(eq("riya@example.com"), any(TaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Design MFE shell"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @WithMockUser(username = "riya@example.com")
    void createTask_returns400WhenTitleMissing() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setDescription("No title provided");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
    }

    @Test
    @WithMockUser(username = "riya@example.com")
    void listTasks_returnsPagedResults() throws Exception {
        TaskResponse task = TaskResponse.builder()
                .id(2L).title("Refactor service layer").status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(taskService.listTasks(eq("riya@example.com"), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(task)));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Refactor service layer"));
    }

    @Test
    void createTask_returns401WhenUnauthenticated() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Should be blocked");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
