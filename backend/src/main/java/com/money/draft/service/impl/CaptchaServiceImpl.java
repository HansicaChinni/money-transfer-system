
package com.money.draft.service.impl;

import com.money.draft.dto.CaptchaChallenge;
import com.money.draft.service.CaptchaService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final long EXPIRY_MS = 5 * 60 * 1000;

    private final SecureRandom random = new SecureRandom();

    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    @Override
    public CaptchaChallenge generateChallenge() {
        int a = random.nextInt(10, 100);
        int b = random.nextInt(10, 100);
        int op = random.nextInt(2);
        String question;
        int answer;
        if (op == 0) {
            question = a + " + " + b;
            answer = a + b;
        } else {
            question = a + " - " + b;
            answer = a - b;
        }
        String token = java.util.UUID.randomUUID().toString();
        store.put(token, new CaptchaEntry(answer, System.currentTimeMillis() + EXPIRY_MS));
        return new CaptchaChallenge(token, question);
    }

    @Override
    public boolean validateCaptcha(String token, String answer) {
        if (token == null || answer == null) return false;
        CaptchaEntry entry = store.remove(token);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expiry) return false;
        try {
            int userAnswer = Integer.parseInt(answer.trim());
            return userAnswer == entry.answer;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private record CaptchaEntry(int answer, long expiry) {}
}
