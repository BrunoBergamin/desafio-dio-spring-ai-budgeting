package dio.budgeting.controller;

import dio.budgeting.dto.response.PageResponse;
import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.TransactionType;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.security.AppUserDetailsService;
import dio.budgeting.service.TransactionService;
import dio.budgeting.support.SecuredWebMvcTest;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SecuredWebMvcTest(TransactionController.class)
@Import(dio.budgeting.security.JwtService.class)
class TransactionControllerTest {

    static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TransactionService transactionService;

    @MockitoBean
    AppUserDetailsService userDetailsService;

    /** Emite um JWT de verdade, para o teste exercitar o decoder real e nao um token de mentira. */
    @Autowired
    dio.budgeting.security.JwtService jwtService;

    /** O site nao manda header nenhum: prova que o cookie sozinho autentica na cadeia real de seguranca. */
    @Test
    void should_authenticate_when_theTokenComesOnlyInTheCookie() throws Exception {
        var userId = UUID.randomUUID();
        var token = jwtService.generate(new dio.budgeting.entity.User("Bruno", "bruno@email.com", "hash") {
            @Override
            public UUID getId() {
                return userId;
            }
        });
        when(transactionService.list(eq(userId), any(), any(), any(), any(), eq(0), eq(50)))
                .thenReturn(new PageResponse<>(java.util.List.of(), 0, 50, 0, 0));

        mockMvc.perform(get("/api/transactions").cookie(new jakarta.servlet.http.Cookie("lumi_token", token)))
                .andExpect(status().isOk());
    }

    @Test
    void should_return401WithProblemDetail_when_tokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));

        verifyNoInteractions(transactionService);
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return201_when_transactionIsCreated() throws Exception {
        var id = UUID.randomUUID();
        when(transactionService.create(eq(UUID.fromString(USER_ID)), any())).thenReturn(new TransactionResponse(
                id, "Mercado", new BigDecimal("80.50"), Category.GROCERIES, "Mercado", TransactionType.EXPENSE, null, LocalDate.of(2026, 9, 15)));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Mercado", "amount": 80.50, "category": "GROCERIES"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/transactions/" + id))
                .andExpect(jsonPath("$.amount").value(80.50))
                .andExpect(jsonPath("$.categoryLabel").value("Mercado"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_returnAPage_when_listingWithPageAndSize() throws Exception {
        var item = new TransactionResponse(UUID.randomUUID(), "Padaria", new BigDecimal("18.50"), Category.GROCERIES, "Mercado", TransactionType.EXPENSE, null, LocalDate.of(2026, 9, 15));
        when(transactionService.list(eq(UUID.fromString(USER_ID)), isNull(), isNull(), isNull(), isNull(), eq(2), eq(10)))
                .thenReturn(new PageResponse<>(java.util.List.of(item), 2, 10, 25, 3));

        mockMvc.perform(get("/api/transactions").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Padaria"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_useFirstPageOf50_when_pagingParamsAreOmitted() throws Exception {
        when(transactionService.list(any(), any(), any(), any(), any(), eq(0), eq(50)))
                .thenReturn(new PageResponse<>(java.util.List.of(), 0, 50, 0, 0));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return400WithFieldErrors_when_requestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/transactions")
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
    @WithMockUser(username = USER_ID)
    void should_return404_when_transactionDoesNotExist() throws Exception {
        var id = UUID.randomUUID();
        when(transactionService.findById(eq(UUID.fromString(USER_ID)), eq(id)))
                .thenThrow(new ResourceNotFoundException("não encontrada"));

        mockMvc.perform(get("/api/transactions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return422_when_periodIsInvalid() throws Exception {
        when(transactionService.summary(any(), any(), any())).thenThrow(new BusinessException("período inválido"));

        mockMvc.perform(get("/api/transactions/summary").param("start", "2026-09-10").param("end", "2026-09-01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("período inválido"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return400_when_bodyHasUnknownCategory() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Pizza", "amount": 50, "category": "PIZZA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return400_when_categoryIsUnknown() throws Exception {
        mockMvc.perform(get("/api/transactions").param("category", "PIZZA"))
                .andExpect(status().isBadRequest());
    }
}
