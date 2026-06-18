
package com.money.draft.dto;

public record CaptchaChallenge(
    String token,
    String question,
    String hint
) {
    public CaptchaChallenge(String token, String question) {
        this(token, question, "Enter the result of the math expression");
    }
}
