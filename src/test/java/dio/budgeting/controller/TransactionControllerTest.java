package dio.budgeting.controller;

import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TransactionService transactionService;

    @Test
    void should_return201_when_transactionIsCreated() throws Exception {
        var id = UUID.randomUUID();
        when(transactionService.create(any())).thenReturn(new TransactionResponse(
                id, "Mercado", new BigDecimal("80.50"), Category.GROCERIES, "Mercado", LocalDate.of(2026, 9, 15)));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Mercado", "amount": 80.50, "category": "GROCERIES"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/transactions/" + id))
                .andExpect(jsonPath("$.amount").value(80.50))
                .andExpect(jsonPath("$.categoryLabel").value("Mercado"));
    }

    @Test
    void should_return400WithFieldErrors_when_requestIsInvalid() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "", "amount": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.category").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void should_return404_when_transactionDoesNotExist() throws Exception {
        var id = UUID.randomUUID();
        when(transactionService.findById(eq(id))).thenThrow(new ResourceNotFoundException("não encontrada"));

        mockMvc.perform(get("/transactions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void should_return422_when_periodIsInvalid() throws Exception {
        when(transactionService.summary(any(), any())).thenThrow(new BusinessException("período inválido"));

        mockMvc.perform(get("/transactions/summary").param("start", "2026-09-10").param("end", "2026-09-01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("período inválido"));
    }

    @Test
    void should_return400_when_bodyHasUnknownCategory() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Pizza", "amount": 50, "category": "PIZZA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
    }

    @Test
    void should_return400_when_categoryIsUnknown() throws Exception {
        mockMvc.perform(get("/transactions").param("category", "PIZZA"))
                .andExpect(status().isBadRequest());
    }
}
