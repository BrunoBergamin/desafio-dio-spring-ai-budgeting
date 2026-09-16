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

/** Limite mensal de gastos de uma categoria. Um por usuario, categoria e mes. */
@Entity
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(
        name = "uk_budgets_user_cat_month", columnNames = {"user_id", "category", "reference_month"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    /** Sempre o dia 1 do mes: DATE e portavel, ordenavel e legivel no banco. */
    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(name = "monthly_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Budget(User user, Category category, LocalDate referenceMonth, BigDecimal monthlyLimit) {
        this.user = user;
        this.category = category;
        this.referenceMonth = referenceMonth.withDayOfMonth(1);
        this.monthlyLimit = monthlyLimit;
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
