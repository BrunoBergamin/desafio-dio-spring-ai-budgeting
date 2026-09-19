package dio.budgeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Meta de economia: guardar um valor ate uma data. O que foi guardado mora aqui, e nao vira lancamento:
 * separar dinheiro para uma meta nao e gastar, entao nao deve entrar no total de gastos nem no orcamento.
 */
@Entity
@Table(name = "savings_goals", uniqueConstraints = @UniqueConstraint(
        name = "uk_goals_user_name", columnNames = {"user_id", "name"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingsGoal {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Unico por usuario: e assim que a Lumi acha a meta pelo nome que a pessoa falou. */
    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;

    @Setter(AccessLevel.NONE)
    @Column(name = "saved_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal savedAmount = BigDecimal.ZERO;

    /** Prazo opcional: sem ele a meta nao vence, so acumula. */
    private LocalDate deadline;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public SavingsGoal(User user, String name, BigDecimal targetAmount, LocalDate deadline) {
        this.user = user;
        this.name = name;
        this.targetAmount = targetAmount;
        this.deadline = deadline;
    }

    /** Guarda mais dinheiro na meta e marca a conclusao quando o alvo e alcancado. */
    public void deposit(BigDecimal amount) {
        changeSaved(savedAmount.add(amount));
    }

    /** Corrige o valor guardado (a pessoa errou a digitacao, tirou dinheiro da meta). */
    public void changeSaved(BigDecimal amount) {
        this.savedAmount = amount.max(BigDecimal.ZERO);
        // Concluida enquanto chegou no alvo; baixar o valor guardado desfaz a conclusao
        this.completedAt = this.savedAmount.compareTo(targetAmount) >= 0
                ? (completedAt != null ? completedAt : Instant.now())
                : null;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
