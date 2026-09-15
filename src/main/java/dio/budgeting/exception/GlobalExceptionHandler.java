package dio.budgeting.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", ex.getMessage());
    }

    @ExceptionHandler(FeatureUnavailableException.class)
    ProblemDetail handleFeatureUnavailable(FeatureUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Recurso indisponível", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        var problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Um ou mais campos estão inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido",
                "JSON malformado ou com valor inválido (verifique a categoria, datas no formato AAAA-MM-DD e o encoding UTF-8)");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Valor '%s' inválido para o parâmetro '%s'".formatted(ex.getValue(), ex.getName()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleUploadSize(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "Arquivo muito grande", "O áudio deve ter no máximo 10MB");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
