package dio.budgeting.mapper;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TransactionMapper {

    public Transaction toEntity(User owner, TransactionRequest request) {
        return new Transaction(
                owner,
                request.description(),
                request.amount(),
                request.category(),
                request.date() != null ? request.date() : LocalDate.now());
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getCategory().getLabel(),
                transaction.getDate());
    }
}
