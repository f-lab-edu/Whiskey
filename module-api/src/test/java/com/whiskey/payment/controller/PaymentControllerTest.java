package com.whiskey.payment.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whiskey.domain.payment.facade.PaymentFacade;
import com.whiskey.domain.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PaymentControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(mock(PaymentService.class), mock(PaymentFacade.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("결제 실패 콜백은 code/message/orderId 파라미터를 정상 바인딩한다")
    void paymentFail_binds_toss_callback_params() throws Exception {
        mockMvc.perform(get("/api/payments/payment-fail")
                .param("code", "PAY_PROCESS_CANCELED")
                .param("message", "사용자가 결제를 취소했습니다.")
                .param("orderId", "order-1234"))
            .andExpect(status().isOk())
            .andExpect(content().string("payment-fail"));
    }

    @Test
    @DisplayName("결제 실패 콜백에 필수 파라미터가 빠지면 400을 반환한다")
    void paymentFail_missing_param_returns_400() throws Exception {
        mockMvc.perform(get("/api/payments/payment-fail")
                .param("code", "PAY_PROCESS_CANCELED")
                .param("message", "사용자가 결제를 취소했습니다."))
            .andExpect(status().isBadRequest());
    }
}