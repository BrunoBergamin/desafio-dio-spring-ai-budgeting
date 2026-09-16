package dio.budgeting.controller;

import dio.budgeting.dto.response.BudgetStatusResponse;
import dio.budgeting.entity.BudgetStatus;
import dio.budgeting.entity.Category;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.security.AppUserDetailsService;
import dio.budgeting.service.BudgetService;
import dio.budgeting.support.SecuredWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SecuredWebMvcTest(BudgetController.class)
class BudgetControllerTest {

    static final String USER_ID = "44444444-4444-4444-4444-444444444444";
    static final UUID USER = UUID.fromString(USER_ID);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BudgetService budgetService;

    @MockitoBean
    AppUserDetailsService userDetailsService;

    @Test
    void should_return401_when_tokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/budgets")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return201_when_budgetIsCreated() throws Exception {
        when(budgetService.create(eq(USER), any())).thenReturn(budgetStatus(BudgetStatus.OK, "0.0"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "GROCERIES", "monthlyLimit": 500.00, "month": "2026-09"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.month").value("2026-09"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return400_when_limitIsMissing() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "GROCERIES", "month": "2026-13"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.monthlyLimit").exists())
                .andExpect(jsonPath("$.errors.month").exists());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return422_when_budgetAlreadyExists() throws Exception {
        when(budgetService.create(eq(USER), any())).thenThrow(new BusinessException("já existe um orçamento"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "GROCERIES", "monthlyLimit": 500.00}
                                """))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_listOnlyAlerts_when_alertsIsCalled() throws Exception {
        when(budgetService.alerts(USER, null)).thenReturn(List.of(budgetStatus(BudgetStatus.WARNING, "85.0")));

        mockMvc.perform(get("/api/budgets/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("WARNING"))
                .andExpect(jsonPath("$[0].usedPercentage").value(85.0));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return404_when_deletingUnknownBudget() throws Exception {
        var id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("não encontrado"))
                .when(budgetService).delete(USER, id);

        mockMvc.perform(delete("/api/budgets/{id}", id)).andExpect(status().isNotFound());
    }

    private BudgetStatusResponse budgetStatus(BudgetStatus status, String used) {
        return new BudgetStatusResponse(UUID.randomUUID(), Category.GROCERIES, "Mercado", "2026-09",
                new BigDecimal("500.00"), new BigDecimal("425.00"), new BigDecimal("75.00"),
                new BigDecimal(used), status, status.getLabel(), "mensagem");
    }
}
