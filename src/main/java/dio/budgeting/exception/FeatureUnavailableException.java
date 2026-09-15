package dio.budgeting.exception;

/** Recurso desligado na configuração atual (ex.: text-to-speech ausente no perfil groq). */
public class FeatureUnavailableException extends RuntimeException {
    public FeatureUnavailableException(String message) {
        super(message);
    }
}
