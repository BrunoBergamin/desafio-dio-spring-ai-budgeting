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
import java.time.YearMonth;
import java.util.UUID;

/**
 * Conta que se repete todo mes (aluguel, Netflix, salario). Nao e um lancamento: e a regra que gera um
 * lancamento por mes, no dia combinado.
 * <p>
 * {@code lastGeneratedMonth} e o que impede duplicar: guarda o ultimo mes ja materializado, entao rodar
 * o gerador duas vezes no mesmo dia nao cria dois lancamentos, e uma parada de dias gera os meses que faltaram.
 */
@Entity
@Table(name = "recurring_transactions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringTransaction {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    /** Derivado da categoria, como na transacao. */
    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /** Dia combinado (1 a 31). Em mes curto, cai no ultimo dia: dia 31 vira 28 em fevereiro. */
    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;

    @Column(nullable = false)
    private boolean active = true;

    /** Primeiro mes valido (dia 1). */
    @Column(name = "start_month", nullable = false)
    private LocalDate startMonth;

    /** Ultimo mes valido (dia 1); nulo significa sem prazo para acabar. */
    @Column(name = "end_month")
    private LocalDate endMonth;

    /** Ultimo mes ja gerado (dia 1); nulo enquanto nunca gerou nada. */
    @Column(name = "last_generated_month")
    private LocalDate lastGeneratedMonth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public RecurringTransaction(User user, String description, BigDecimal amount, Category category,
                                int dayOfMonth, LocalDate startMonth, LocalDate endMonth) {
        this.user = user;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.type = category.getType();
        this.dayOfMonth = dayOfMonth;
        this.startMonth = startMonth.withDayOfMonth(1);
        this.endMonth = endMonth == null ? null : endMonth.withDayOfMonth(1);
    }

    public void changeCategory(Category category) {
        this.category = category;
        this.type = category.getType();
    }

    public void setEndMonth(LocalDate endMonth) {
        this.endMonth = endMonth == null ? null : endMonth.withDayOfMonth(1);
    }

    /** Data do lancamento naquele mes. Dia 31 em fevereiro vira 28 (ou 29 em ano bissexto). */
    public LocalDate occurrenceIn(YearMonth month) {
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
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
