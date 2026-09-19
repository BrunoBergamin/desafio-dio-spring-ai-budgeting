package dio.budgeting.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Pagina de resultados no JSON. E um record proprio, e nao o {@code Page} do Spring Data, para o contrato
 * da API nao mudar quando o Spring mudar a serializacao dele.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
