package dio.budgeting.controller;

import dio.budgeting.dto.response.SavingsGoalResponse;
import dio.budgeting.entity.GoalStatus;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.security.AppUserDetailsService;
import dio.budgeting.service.SavingsGoalService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SecuredWebMvcTest(SavingsGoalController.class)
class SavingsGoalControllerTest {

    static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired MockMvc mockMvc;

    @MockitoBean SavingsGoalService goalService;
    @MockitoBean AppUserDetailsService userDetailsService;

    private SavingsGoalResponse viagem() {
        return new SavingsGoalResponse(UUID.randomUUID(), "Viagem", new BigDecimal("6000.00"),
                new BigDecimal("1800.00"), new BigDecimal("4200.00"), new BigDecimal("30.0"),
                LocalDate.of(2027, 1, 31), 5L, new BigDecimal("840.00"),
                GoalStatus.IN_PROGRESS, "Em andamento", "você já guardou 30.0% da meta Viagem");
    }

    @Test
    void should_return401WithProblemDetail_when_tokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));

        verifyNoInteractions(goalService);
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_listGoalsWithProgress_when_authenticated() throws Exception {
        when(goalService.list(UUID.fromString(USER_ID))).thenReturn(List.of(viagem()));

        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Viagem"))
                .andExpect(jsonPath("$[0].percentage").value(30.0))
                .andExpect(jsonPath("$[0].suggestedMonthly").value(840.00));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return201_when_goalIsCreated() throws Exception {
        when(goalService.create(eq(UUID.fromString(USER_ID)), any())).thenReturn(viagem());

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Viagem", "targetAmount": 6000.00, "deadline": "2027-01-31"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return200_when_moneyIsSavedIntoTheGoal() throws Exception {
        var id = UUID.randomUUID();
        when(goalService.deposit(eq(UUID.fromString(USER_ID)), eq(id), eq(new BigDecimal("300.00"))))
                .thenReturn(viagem());

        mockMvc.perform(post("/api/goals/{id}/deposits", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 300.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAmount").value(1800.00));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return400_when_theDepositIsZero() throws Exception {
        mockMvc.perform(post("/api/goals/{id}/deposits", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        verifyNoInteractions(goalService);
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return422_when_theGoalNameIsRepeated() throws Exception {
        when(goalService.create(any(), any())).thenThrow(new BusinessException("você já tem uma meta chamada Viagem"));

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Viagem", "targetAmount": 6000.00}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("você já tem uma meta chamada Viagem"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return404_when_theGoalDoesNotExist() throws Exception {
        var id = UUID.randomUUID();
        when(goalService.deposit(any(), eq(id), any())).thenThrow(new ResourceNotFoundException("Meta não encontrada"));

        mockMvc.perform(post("/api/goals/{id}/deposits", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.00}
                                """))
                .andExpect(status().isNotFound());
    }
}
