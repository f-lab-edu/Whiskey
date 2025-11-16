package com.whiskey;

import com.whiskey.annotation.CurrentMemberId;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TestController {
    @GetMapping("/api/test")
    public ResponseEntity<Map<String, Object>> test(@CurrentMemberId Long memberId) {
        Map<String, Object> response = new HashMap<>();
        response.put("memberId", memberId);
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/test/member-id-check")
    public ResponseEntity<Long> memberIdCheck(@CurrentMemberId Long memberId) {
        return ResponseEntity.ok(memberId);
    }

    @Value("${toss.payment.client-key}")
    private String tossClientKey;

    @GetMapping("/api/test/payment-config")
    public ResponseEntity<Map<String, Object>> paymentConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("memberId", 3L);
        config.put("clientKey", tossClientKey);
        return ResponseEntity.ok(config);
    }
}