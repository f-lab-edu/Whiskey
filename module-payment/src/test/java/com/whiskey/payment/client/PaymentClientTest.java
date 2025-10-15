package com.whiskey.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiskey.payment.config.PaymentProperties;
import com.whiskey.payment.dto.PaymentConfirmRequest;
import com.whiskey.payment.dto.PaymentResponse;
import com.whiskey.payment.exception.RetryablePaymentException;
import com.whiskey.payment.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PaymentProperties properties;

    private PaymentClient paymentClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        paymentClient = new PaymentClient(
            restTemplate,
            objectMapper,
            properties
        );

        given(properties.getBaseUrl()).willReturn("https://api.tosspayments.com/v1/payments/confirm");
        given(properties.getSecretKey()).willReturn("secretKey");
    }

    @Test
    void 결제_승인_성공() throws JsonProcessingException {
        PaymentConfirmRequest request = createRequest();

        PaymentResponse mockResponse = new PaymentResponse(
            "test_payment_key",
            "ORDER_123",
            10000L
        );

        ResponseEntity<PaymentResponse> responseEntity =
            ResponseEntity.ok(mockResponse);

        given(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(PaymentResponse.class)
        )).willReturn(responseEntity);

        PaymentResponse response = paymentClient.confirmPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.paymentKey()).isEqualTo("test_payment_key");
        assertThat(response.orderId()).isEqualTo("ORDER_123");
        assertThat(response.amount()).isEqualTo(10000L);
    }

    @Test
    void 잘못된_요청() {
        PaymentConfirmRequest request = createRequest();

        String errorJson = "{\"code\":\"INVALID_REQUEST\",\"message\":\"잘못된 요청입니다.\"}";
        HttpClientErrorException exception =
            HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                errorJson.getBytes(),
                null
            );

        given(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(PaymentResponse.class)
        )).willThrow(exception);

        assertThatThrownBy(() -> paymentClient.confirmPayment(request))
            .isInstanceOf(PaymentException.class)
            .hasMessageContaining("잘못된 요청");
    }

    private PaymentConfirmRequest createRequest() {
        return PaymentConfirmRequest.builder()
            .paymentKey("test_payment_key")
            .orderId("ORDER_123")
            .amount(10000L)
            .build();
    }

    @Test
    void 에러_발생_후_재시도() {
        PaymentConfirmRequest request = createRequest();

        HttpServerErrorException exception =
            HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                null,
                null,
                null
            );

        given(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(PaymentResponse.class)
        )).willThrow(exception);

        assertThatThrownBy(() -> paymentClient.confirmPayment(request))
            .isInstanceOf(RetryablePaymentException.class);
    }
}