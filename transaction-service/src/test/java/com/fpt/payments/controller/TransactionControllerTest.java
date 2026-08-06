package com.fpt.payments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.payments.dto.TransferRequest;
import com.fpt.payments.dto.TransferResponse;
import com.fpt.payments.enums.TransactionStatus;
import com.fpt.payments.security.JwtUtil;
import com.fpt.payments.service.TransactionService;
import com.fpt.payments.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(TransactionControllerTest.MethodSecurityConfig.class)
class TransactionControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TransactionService transactionService;
    @MockBean TransactionRepository transactionRepository;
    @MockBean JwtUtil jwtUtil;

    @Test
    void initiateTransfer_withoutAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/transfers/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void initiateTransfer_withNegativeAmount_returns422() throws Exception {
        var req = new TransferRequest(UUID.randomUUID(), new BigDecimal("-100"), "USD", "key-1", null);
        mockMvc.perform(post("/api/transfers/" + UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void initiateTransfer_valid_returns202() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        var req = new TransferRequest(toId, new BigDecimal("500"), "USD", "key-abc", "test");
        var resp = new TransferResponse(txId, fromId, toId, new BigDecimal("500"), "USD",
                TransactionStatus.PENDING_FRAUD_CHECK, "key-abc", "test", Instant.now(), Instant.now());

        when(transactionService.initiateTransfer(any(), any())).thenReturn(resp);
        when(transactionRepository.findById(txId)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/api/transfers/" + fromId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_FRAUD_CHECK"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listAll_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/transfers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BANK_ADMIN")
    void listAll_adminRole_returns200() throws Exception {
        when(transactionRepository.findAll()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/transfers"))
                .andExpect(status().isOk());
    }
}
