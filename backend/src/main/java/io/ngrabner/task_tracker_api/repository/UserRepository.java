package io.ngrabner.task_tracker_api.repository;

import java.util.Optional;
import io.ngrabner.task_tracker_api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleSub(String googleSub);
}
