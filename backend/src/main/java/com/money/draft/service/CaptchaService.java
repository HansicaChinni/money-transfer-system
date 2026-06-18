
package com.money.draft.service;

import com.money.draft.dto.CaptchaChallenge;

public interface CaptchaService {
    CaptchaChallenge generateChallenge();
    boolean validateCaptcha(String token, String answer);
}
