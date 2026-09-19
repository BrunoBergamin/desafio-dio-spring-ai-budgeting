package dio.budgeting.controller;

import dio.budgeting.dto.response.RecurringResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.TransactionType;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.security.AppUserDetailsService;
import dio.budgeting.service.RecurringTransactionService;
import dio.budgeting.support.SecuredWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SecuredWebMvcTest(RecurringTransactionController.class)
class RecurringTransactionControllerTest {

    static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired MockMvc mockMvc;

    @MockitoBean RecurringTransactionService recurringService;
    @MockitoBean AppUserDetailsService userDetailsService;

    private RecurringResponse aluguel() {
        return new RecurringResponse(UUID.randomUUID(), "Aluguel", new BigDecimal("1500.00"), Category.HOUSING,
                "Moradia e contas de casa", TransactionType.EXPENSE, 10, true, "2026-09", null,
                LocalDate.of(2026, 10, 10), "Aluguel de 1500.00 reais todo dia 10");
    }

    @Test
    void should_return401WithProblemDetail_when_tokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/recurring"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));

        verifyNoInteractions(recurringService);
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_listTheRules_when_authenticated() throws Exception {
        when(recurringService.list(UUID.fromString(USER_ID))).thenReturn(List.of(aluguel()));

        mockMvc.perform(get("/api/recurring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Aluguel"))
                .andExpect(jsonPath("$[0].dayOfMonth").value(10))
                .andExpect(jsonPath("$[0].nextOccurrence").value("2026-10-10"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return201_when_ruleIsCreated() throws Exception {
        when(recurringService.create(eq(UUID.fromString(USER_ID)), any())).thenReturn(aluguel());

        mockMvc.perform(post("/api/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Aluguel", "amount": 1500.00, "category": "HOUSING", "dayOfMonth": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return400_when_dayIsOutOfRange() throws Exception {
        mockMvc.perform(post("/api/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Aluguel", "amount": 1500.00, "category": "HOUSING", "dayOfMonth": 32}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dayOfMonth").exists());

        verifyNoInteractions(recurringService);
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return404_when_ruleBelongsToSomeoneElse() throws Exception {
        var id = UUID.randomUUID();
        when(recurringService.update(eq(UUID.fromString(USER_ID)), eq(id), any()))
                .thenThrow(new ResourceNotFoundException("não encontrada"));

        mockMvc.perform(put("/api/recurring/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Aluguel", "amount": 1500.00, "category": "HOUSING", "dayOfMonth": 10}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return204_when_ruleIsDeleted() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(delete("/api/recurring/{id}", id).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}
