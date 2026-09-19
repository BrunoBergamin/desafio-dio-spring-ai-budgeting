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

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    /** Gravado como texto (36 chars) para o mesmo schema servir H2 e MySQL. */
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String description;

    /** Valor em reais, com duas casas decimais. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Sem setter: categoria e tipo andam juntos e mudam por {@link #changeCategory(Category)}. */
    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    /** Gasto ou receita, derivado da categoria. */
    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate date;

    /** Dono do lançamento: toda consulta filtra por ele. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    /** O tipo vem da categoria: RESTAURANT e gasto, SALARY e receita. */
    public Transaction(User user, String description, BigDecimal amount, Category category, LocalDate date) {
        this.user = user;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.type = category.getType();
        this.date = date;
    }

    /** Mantem o tipo em dia quando a categoria muda numa edicao. */
    public void changeCategory(Category category) {
        this.category = category;
        this.type = category.getType();
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
