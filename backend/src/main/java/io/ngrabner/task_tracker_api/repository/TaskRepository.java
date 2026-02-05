package io.ngrabner.task_tracker_api.repository;

import io.ngrabner.task_tracker_api.domain.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT t FROM Task t
        WHERE t.userId = :userId
          AND (COALESCE(:query, '') = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (COALESCE(:status, '') = '' OR t.status = :status)
        """)
    Page<Task> searchTasks(
            @Param("userId") Long userId,
            @Param("query") String query,
            @Param("status") String status,
            Pageable pageable
    );
}
