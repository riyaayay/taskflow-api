package com.riya.taskflow.service;

import com.riya.taskflow.dto.TaskRequest;
import com.riya.taskflow.dto.TaskResponse;
import com.riya.taskflow.exception.ResourceNotFoundException;
import com.riya.taskflow.model.Task;
import com.riya.taskflow.model.TaskPriority;
import com.riya.taskflow.model.TaskStatus;
import com.riya.taskflow.model.User;
import com.riya.taskflow.repository.TaskRepository;
import com.riya.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Riya").email("riya@example.com").password("hashed").build();
    }

    @Test
    void createTask_savesTaskForOwner() {
        when(userRepository.findByEmail("riya@example.com")).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(10L);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        TaskRequest request = new TaskRequest();
        request.setTitle("Write unit tests");
        request.setDescription("Cover TaskService with JUnit + Mockito");
        request.setPriority(TaskPriority.HIGH);

        TaskResponse response = taskService.createTask("riya@example.com", request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Write unit tests");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_throwsWhenOwnerNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        TaskRequest request = new TaskRequest();
        request.setTitle("Orphan task");

        assertThatThrownBy(() -> taskService.createTask("ghost@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getTask_throwsWhenTaskBelongsToAnotherUser() {
        User otherOwner = User.builder().id(2L).name("Other").email("other@example.com").build();
        Task task = Task.builder().id(5L).title("Not yours").owner(otherOwner).build();

        when(userRepository.findByEmail("riya@example.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getTask("riya@example.com", 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTask_removesOwnedTask() {
        Task task = Task.builder().id(7L).title("Delete me").owner(owner).build();

        when(userRepository.findByEmail("riya@example.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));

        taskService.deleteTask("riya@example.com", 7L);

        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    void updateTask_appliesPartialStatusChangeOnly() {
        Task existing = Task.builder()
                .id(3L).title("Old title").description("Old desc")
                .status(TaskStatus.TODO).priority(TaskPriority.LOW)
                .owner(owner)
                .build();

        when(userRepository.findByEmail("riya@example.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest request = new TaskRequest();
        request.setTitle("New title");
        request.setDescription("New desc");
        request.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse response = taskService.updateTask("riya@example.com", 3L, request);

        assertThat(response.getTitle()).isEqualTo("New title");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.getPriority()).isEqualTo(TaskPriority.LOW); // unchanged since request omitted it
    }
}
