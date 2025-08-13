package com.vacutrack.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    private static final Pattern CEDULA_PATTERN = Pattern.compile(
            "^[0-9]{10}$"
    );

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isValidCedula(String cedula) {
        if (cedula == null) return false;
        return CEDULA_PATTERN.matcher(cedula).matches();
    }
}