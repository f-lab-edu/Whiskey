package com.whiskey.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiskey.payment.config.TossPaymentProperties;
import com.whiskey.payment.dto.TossPaymentConfirmRequest;
import com.whiskey.payment.dto.TossPaymentErrorResponse;
import com.whiskey.payment.dto.TossPaymentResponse;
import com.whiskey.payment.exception.RetryablePaymentException;
import com.whiskey.payment.exception.TossPaymentException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TossPaymentProperties properties;

    public TossPaymentResponse confirmPayment(TossPaymentConfirmRequest request)
        throws JsonProcessingException {
        String requestUrl = properties.getBaseUrl();

        log.info("Toss payment 결제 요청 - orderId: {}, amount: {}", request.orderId(), request.amount());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String auth = properties.getSecretKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<TossPaymentConfirmRequest> requestEntity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<TossPaymentResponse> response = restTemplate.postForEntity(requestUrl, requestEntity, TossPaymentResponse.class);
            return response.getBody();
        }
        catch (HttpClientErrorException e) {
            // 4XX 에러, Retry 불가처리
            log.error("Toss payment 클라이언트 에러 - orderId: {}, status: {}, body: {}", request.orderId(), e.getStatusCode(), e.getResponseBodyAsString());
            TossPaymentErrorResponse errorResponse = objectMapper.readValue(e.getResponseBodyAsString(), TossPaymentErrorResponse.class);
            throw new TossPaymentException(
                errorResponse.code(),
                errorResponse.message()
            );
        }
        catch (HttpServerErrorException e) {
            // 5XX 에러, Retry 가능
            log.warn("Toss payment 서버 에러 - orderId: {}, status: {}", request.orderId(), e.getStatusCode());
            throw new RetryablePaymentException("Toss payment 서버 에러 : " + e.getStatusCode());
        }
        catch (Exception e) {
            // 기타 에러, Retry 가능
            log.warn("Toss payment network error - orderId: {}, message: {}", request.orderId(), e.getMessage());
            throw new RetryablePaymentException("기타 에러 : " + e.getMessage());
        }
    }
}
