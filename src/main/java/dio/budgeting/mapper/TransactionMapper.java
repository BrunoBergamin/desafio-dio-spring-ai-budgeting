package dio.budgeting.mapper;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final Clock clock;

    public Transaction toEntity(User owner, TransactionRequest request) {
        return new Transaction(
                owner,
                request.description(),
                request.amount(),
                request.category(),
                request.date() != null ? request.date() : LocalDate.now(clock));
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getCategory().getLabel(),
                transaction.getType(),
                transaction.getRecurring() == null ? null : transaction.getRecurring().getId(),
                transaction.getDate());
    }
}
