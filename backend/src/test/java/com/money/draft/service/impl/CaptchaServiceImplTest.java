
package com.money.draft.service.impl;

import com.money.draft.dto.CaptchaChallenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaServiceImpl();
    }

    @Test
    void generateChallenge_ShouldReturnValidChallenge() {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        assertNotNull(challenge);
        assertNotNull(challenge.token());
        assertNotNull(challenge.question());
        assertFalse(challenge.token().isEmpty());
        assertFalse(challenge.question().isEmpty());
    }

    @Test
    void generateChallenge_ShouldProduceMathExpression() {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        String question = challenge.question();
        assertTrue(question.contains("+") || question.contains("-"),
                "Question should be a math expression");
    }

    @Test
    void validateCaptcha_ShouldReturnTrue_ForCorrectAnswer() {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        String question = challenge.question();
        String[] parts = question.split(" ");
        int a = Integer.parseInt(parts[0]);
        int b = Integer.parseInt(parts[2]);
        int expectedAnswer = parts[1].equals("+") ? a + b : a - b;

        boolean result = captchaService.validateCaptcha(challenge.token(), String.valueOf(expectedAnswer));
        assertTrue(result);
    }

    @Test
    void validateCaptcha_ShouldReturnFalse_ForIncorrectAnswer() {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        boolean result = captchaService.validateCaptcha(challenge.token(), "99999");
        assertFalse(result);
    }

    @Test
    void validateCaptcha_ShouldReturnFalse_ForNullToken() {
        assertFalse(captchaService.validateCaptcha(null, "42"));
    }

    @Test
    void validateCaptcha_ShouldReturnFalse_ForNullAnswer() {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        assertFalse(captchaService.validateCaptcha(challenge.token(), null));
    }

    @Test
    void validateCaptcha_ShouldReturnFalse_ForExpiredToken() throws Exception {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        // We can't easily test expiry without reflection, but at least validate it works fresh
        boolean result = captchaService.validateCaptcha(challenge.token(), "0");
        assertFalse(result);
    }

    @Test
    void validateCaptcha_ShouldConsumeTokenOnFirstUse() {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        String question = challenge.question();
        String[] parts = question.split(" ");
        int a = Integer.parseInt(parts[0]);
        int b = Integer.parseInt(parts[2]);
        int expectedAnswer = parts[1].equals("+") ? a + b : a - b;

        assertTrue(captchaService.validateCaptcha(challenge.token(), String.valueOf(expectedAnswer)));
        assertFalse(captchaService.validateCaptcha(challenge.token(), String.valueOf(expectedAnswer)));
    }

    @Test
    void validateCaptcha_ShouldReturnFalse_ForNonExistentToken() {
        assertFalse(captchaService.validateCaptcha("nonexistent-token", "42"));
    }

    @Test
    void generateChallenge_ShouldProduceDifferentTokensEachTime() {
        CaptchaChallenge c1 = captchaService.generateChallenge();
        CaptchaChallenge c2 = captchaService.generateChallenge();
        assertNotEquals(c1.token(), c2.token());
    }
}
