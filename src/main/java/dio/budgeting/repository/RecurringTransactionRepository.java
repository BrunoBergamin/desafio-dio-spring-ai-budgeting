package dio.budgeting.repository;

import dio.budgeting.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

    Optional<RecurringTransaction> findByIdAndUserId(UUID id, UUID userId);

    List<RecurringTransaction> findAllByUserIdOrderByDayOfMonth(UUID userId);

    /**
     * Unico metodo sem usuario do projeto: o gerador diario roda por todo mundo, nao por quem esta logado.
     * Fica explicito no nome que ele so enxerga regras ligadas.
     */
    List<RecurringTransaction> findAllByActiveTrue();

    long countByUserId(UUID userId);
}
