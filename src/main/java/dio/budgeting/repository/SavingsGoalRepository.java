package dio.budgeting.repository;

import dio.budgeting.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {

    Optional<SavingsGoal> findByIdAndUserId(UUID id, UUID userId);

    /** A Lumi acha a meta pelo nome que a pessoa falou, sem se importar com maiusculas. */
    Optional<SavingsGoal> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    List<SavingsGoal> findAllByUserIdOrderByCompletedAtAscDeadlineAscCreatedAtAsc(UUID userId);

    long countByUserId(UUID userId);
}
