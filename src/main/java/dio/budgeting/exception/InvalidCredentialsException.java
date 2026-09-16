package dio.budgeting.exception;

/** E-mail ou senha incorretos. A mensagem e generica de proposito, para nao revelar qual dos dois. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("e-mail ou senha incorretos");
    }
}
