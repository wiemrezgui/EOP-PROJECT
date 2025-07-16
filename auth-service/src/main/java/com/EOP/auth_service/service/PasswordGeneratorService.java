package com.EOP.auth_service.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class PasswordGeneratorService {

    private static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_CHARACTERS = UPPERCASE_LETTERS + LOWERCASE_LETTERS + SYMBOLS;
    private static final int PASSWORD_LENGTH = 10;
    private final SecureRandom random = new SecureRandom();

    public String generatePassword() {
        StringBuilder password = new StringBuilder();
        String generatedPassword;
        do{
            // Ensure at least one character from each category
        password.append(getRandomCharacter(UPPERCASE_LETTERS));
        password.append(getRandomCharacter(LOWERCASE_LETTERS));
        password.append(getRandomCharacter(SYMBOLS));

        // Fill remaining positions with random characters from all categories
        for (int i = 3; i < PASSWORD_LENGTH; i++) {
            password.append(getRandomCharacter(ALL_CHARACTERS));
        }

        // Shuffle the password to avoid predictable patterns
         generatedPassword = shuffleString(password.toString());}
        while(this.validatePassword(generatedPassword));
        return generatedPassword;
    }

    private char getRandomCharacter(String characterSet) {
        int randomIndex = random.nextInt(characterSet.length());
        return characterSet.charAt(randomIndex);
    }

    private String shuffleString(String input) {
        char[] characters = input.toCharArray();

        // Fisher-Yates shuffle algorithm
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }

        return new String(characters);
    }

    public boolean validatePassword(String password) {
        if (password == null || password.length() != PASSWORD_LENGTH) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (UPPERCASE_LETTERS.indexOf(c) != -1) {
                hasUppercase = true;
            } else if (LOWERCASE_LETTERS.indexOf(c) != -1) {
                hasLowercase = true;
            } else if (SYMBOLS.indexOf(c) != -1) {
                hasSymbol = true;
            }
        }

        return hasUppercase && hasLowercase && hasSymbol;
    }
}