package com.whiskey;

import com.whiskey.annotation.CurrentMemberId;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
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
}