package dio.budgeting.exception;

/** Violação de regra de negócio (ex.: valor inválido, período invertido). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
