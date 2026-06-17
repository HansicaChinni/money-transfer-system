
package com.money.draft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.money.draft.dto.*;
import com.money.draft.exception.BusinessException;
import com.money.draft.exception.GlobalExceptionHandler;
import com.money.draft.service.RewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminRewardControllerTest {

    @Mock
    private RewardService rewardService;

    @InjectMocks
    private AdminRewardController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getRedemptions_ShouldReturnList() throws Exception {
        when(rewardService.getAllRedemptions()).thenReturn(List.of(
                new RedemptionResponse(10L, 1L, "Gift", "Brand", 100, new BigDecimal("50"),
                        "PENDING", null, null, Instant.now(), null)
        ));

        mockMvc.perform(get("/api/admin/rewards/redemptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)));
    }

    @Test
    void fulfill_ShouldSucceed() throws Exception {
        var resp = new RedemptionResponse(10L, 1L, "Gift", "Brand", 100, new BigDecimal("50"),
                "FULFILLED", "RWD-ABC-123", "thanks", Instant.now(), null);
        when(rewardService.fulfillRedemption(10L, "thanks")).thenReturn(resp);

        mockMvc.perform(patch("/api/admin/rewards/redemptions/10/fulfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillRequest("thanks"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FULFILLED")))
                .andExpect(jsonPath("$.couponCode", is("RWD-ABC-123")));
    }

    @Test
    void fulfill_ShouldReturn400_WhenRedemptionNotFound() throws Exception {
        when(rewardService.fulfillRedemption(99L, "notes"))
                .thenThrow(new BusinessException("REDEMPTION_NOT_FOUND", "Redemption not found"));

        mockMvc.perform(patch("/api/admin/rewards/redemptions/99/fulfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillRequest("notes"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancel_ShouldSucceed() throws Exception {
        var resp = new RedemptionResponse(10L, 1L, "Gift", "Brand", 100, new BigDecimal("50"),
                "CANCELLED", null, null, Instant.now(), null);
        when(rewardService.cancelRedemption(10L)).thenReturn(resp);

        mockMvc.perform(patch("/api/admin/rewards/redemptions/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void getItems_ShouldReturnList() throws Exception {
        when(rewardService.getAvailableItems()).thenReturn(List.of(
                new RewardItemResponse(1L, "Gift", "desc", "Brand", 100, new BigDecimal("50"), true, null)
        ));

        mockMvc.perform(get("/api/admin/rewards/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Gift")));
    }

    @Test
    void createItem_ShouldReturn201() throws Exception {
        var req = new CreateRewardItemRequest("Gift", "desc", "Brand", 100, new BigDecimal("50"), "img.jpg");
        var resp = new RewardItemResponse(1L, "Gift", "desc", "Brand", 100, new BigDecimal("50"), true, "img.jpg");
        when(rewardService.createItem(any())).thenReturn(resp);

        mockMvc.perform(post("/api/admin/rewards/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.imageUrl", is("img.jpg")));
    }

    @Test
    void createItem_ShouldReturn400_WhenValidationFails() throws Exception {
        var req = new CreateRewardItemRequest("", null, "", -1, null, null);

        mockMvc.perform(post("/api/admin/rewards/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItem_ShouldSucceed() throws Exception {
        var req = new CreateRewardItemRequest("New", "new desc", "NewBrand", 200, new BigDecimal("100"), null);
        var resp = new RewardItemResponse(1L, "New", "new desc", "NewBrand", 200, new BigDecimal("100"), true, null);
        when(rewardService.updateItem(1L, req)).thenReturn(resp);

        mockMvc.perform(put("/api/admin/rewards/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New")))
                .andExpect(jsonPath("$.pointsRequired", is(200)));
    }

    @Test
    void deleteItem_ShouldReturn204() throws Exception {
        doNothing().when(rewardService).deleteItem(1L);

        mockMvc.perform(delete("/api/admin/rewards/items/1"))
                .andExpect(status().isNoContent());

        verify(rewardService).deleteItem(1L);
    }
}
