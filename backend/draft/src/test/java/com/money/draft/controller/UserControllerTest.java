package com.money.draft.controller;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.MeTransferRequest;
import com.money.draft.dto.TransferResponse;
import com.money.draft.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = true)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserRepository userRepository;

    @MockitoBean
    private TransferService transferService;

    @Test
    @WithMockUser(username = "test1", roles = "USER")
    void transfer_success_forAuthenticatedUser() throws Exception {
        // --- mock authenticated user ---
        AppUser user = new AppUser();
        user.setUsername("test1");
        user.setAccountId(42L);

        when(userRepository.findByUsername("test1"))
                .thenReturn(Optional.of(user));

        // --- mock transfer response ---
        TransferResponse response =
                TransferResponse.success(1001L, BigDecimal.valueOf(250));

        when(transferService.transferForUser(
                anyLong(),
                anyLong(),
                any(BigDecimal.class)
        )).thenReturn(response);

        mockMvc.perform(post("/me/transfer")
                        .principal(() -> "test1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
        {
          "toAccountId": 99,
          "amount": 250
        }
        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value(1001))
                .andExpect(jsonPath("$.amount").value(250));
    }
}

