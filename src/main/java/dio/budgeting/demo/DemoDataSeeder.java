package dio.budgeting.demo;

import dio.budgeting.entity.Budget;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.RecurringTransaction;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.User;
import dio.budgeting.repository.BudgetRepository;
import dio.budgeting.repository.RecurringTransactionRepository;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Cria a conta demo com dois meses de gastos ficticios e alguns orcamentos, uma unica vez.
 * Os dados sao inventados de proposito: servem para a Lumi ter o que responder logo de cara.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final DemoProperties properties;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final RecurringTransactionRepository recurringRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private record Sample(String description, String amount, Category category, int daysAgo) {
    }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("Compras da semana no mercado", "312.40", Category.GROCERIES, 1),
            new Sample("Padaria", "18.50", Category.GROCERIES, 2),
            new Sample("Almoço no restaurante", "48.90", Category.RESTAURANT, 2),
            new Sample("Uber para o trabalho", "22.90", Category.TRANSPORT, 3),
            new Sample("Farmácia: remédio para dor de cabeça", "27.30", Category.PHARMA, 5),
            new Sample("Gasolina", "180.00", Category.AUTO, 6),
            new Sample("Pizza de sexta", "72.00", Category.RESTAURANT, 7),
            new Sample("Conta de luz", "189.75", Category.HOUSING, 8),
            new Sample("Internet", "119.90", Category.HOUSING, 8),
            new Sample("Cinema", "64.00", Category.LEISURE, 9),
            new Sample("Ração do cachorro", "139.90", Category.PETS, 10),
            new Sample("Mercado", "204.15", Category.GROCERIES, 12),
            new Sample("Spotify", "21.90", Category.SUBSCRIPTIONS, 13),
            new Sample("Corte de cabelo", "45.00", Category.PERSONAL_CARE, 14),
            new Sample("Ônibus", "9.60", Category.TRANSPORT, 15),
            new Sample("Curso online", "89.00", Category.EDUCATION, 17),
            new Sample("Presente de aniversário", "120.00", Category.GIFTS, 19),
            new Sample("Lanche", "24.50", Category.RESTAURANT, 21),
            new Sample("Tênis novo", "259.90", Category.CLOTHING, 24),
            // mes anterior, para o painel ter comparacao
            new Sample("Mercado", "298.70", Category.GROCERIES, 33),
            new Sample("Conta de luz", "176.20", Category.HOUSING, 36),
            new Sample("Internet", "119.90", Category.HOUSING, 36),
            new Sample("Gasolina", "165.00", Category.AUTO, 38),
            new Sample("Jantar de comemoração", "142.00", Category.RESTAURANT, 40),
            new Sample("Veterinário", "210.00", Category.PETS, 44),
            new Sample("Mercado", "254.30", Category.GROCERIES, 47),
            new Sample("Academia", "99.90", Category.SUBSCRIPTIONS, 50),
            new Sample("IPVA (parcela)", "230.00", Category.TAXES, 52),
            // receitas, para o painel ter saldo e nao so gasto
            new Sample("Projeto freelance", "800.00", Category.FREELANCE, 22),
            new Sample("Rendimento da poupança", "63.40", Category.INVESTMENTS, 46));

    /**
     * Semeia por bloco, e nao "pula se a conta existe": com o MySQL guardando os dados em volume, uma versao
     * nova do projeto precisa conseguir acrescentar o que ainda nao existe (receitas, por exemplo) numa
     * conta demo criada por uma versao anterior.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var user = userRepository.findByEmail(properties.email()).orElseGet(() -> {
            var created = userRepository.save(new User(properties.name(), properties.email(),
                    passwordEncoder.encode(properties.password())));
            log.info("[demo] conta demo criada: {} (senha no .env.example)", properties.email());
            return created;
        });
        var today = LocalDate.now(clock);

        if (transactionRepository.findTop5ByUserIdOrderByDateDescCreatedAtDesc(user.getId()).isEmpty()) {
            for (var sample : SAMPLES) {
                transactionRepository.save(new Transaction(user, sample.description(),
                        new BigDecimal(sample.amount()), sample.category(), today.minusDays(sample.daysAgo())));
            }
            log.info("[demo] {} lançamentos de exemplo criados", SAMPLES.size());
        }

        // Contas fixas: alem de aparecerem na pagina "Recorrentes", elas geram os lancamentos
        // de Netflix e salario sozinhas na subida, mostrando o recurso funcionando de verdade.
        if (recurringRepository.countByUserId(user.getId()) == 0) {
            var twoMonthsAgo = today.minusMonths(2).withDayOfMonth(1);
            recurringRepository.save(new RecurringTransaction(user, "Aluguel", new BigDecimal("1500.00"),
                    Category.HOUSING, 10, twoMonthsAgo, null));
            recurringRepository.save(new RecurringTransaction(user, "Netflix", new BigDecimal("59.90"),
                    Category.SUBSCRIPTIONS, 4, twoMonthsAgo, null));
            recurringRepository.save(new RecurringTransaction(user, "Salário", new BigDecimal("5200.00"),
                    Category.SALARY, 5, twoMonthsAgo, null));
            log.info("[demo] 3 contas recorrentes de exemplo criadas");
        }

        var month = today.withDayOfMonth(1);
        if (budgetRepository.findAllByUserIdAndReferenceMonthOrderByCategory(user.getId(), month).isEmpty()) {
            budgetRepository.save(new Budget(user, Category.GROCERIES, month, new BigDecimal("700.00")));
            budgetRepository.save(new Budget(user, Category.RESTAURANT, month, new BigDecimal("300.00")));
            budgetRepository.save(new Budget(user, Category.SUBSCRIPTIONS, month, new BigDecimal("100.00")));
            budgetRepository.save(new Budget(user, Category.TRANSPORT, month, new BigDecimal("150.00")));
            log.info("[demo] 4 orçamentos de exemplo criados para {}", month);
        }
    }
}
