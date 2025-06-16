package com.bitcoinminer.app;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Code Validator and Error Corrector for Bitcoin Miner App
 * Prevents parser errors and validates input data
 */
public class CodeValidator {
    
    private static final String TAG = "CodeValidator";
    
    // Bitcoin address validation patterns
    private static final Pattern BITCOIN_ADDRESS_PATTERN = Pattern.compile(
        "^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$|^bc1[a-z0-9]{39,59}$"
    );
    
    // Input sanitization patterns
    private static final Pattern SAFE_INPUT_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9.]+$");
    
    /**
     * Validates Bitcoin wallet address format
     */
    public static ValidationResult validateBitcoinAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new ValidationResult(false, "Bitcoin address cannot be empty");
        }
        
        String cleanAddress = sanitizeInput(address);
        
        // Check length constraints
        if (cleanAddress.length() < 26 || cleanAddress.length() > 62) {
            return new ValidationResult(false, "Invalid Bitcoin address length");
        }
        
        // Validate format
        if (!BITCOIN_ADDRESS_PATTERN.matcher(cleanAddress).matches()) {
            return new ValidationResult(false, "Invalid Bitcoin address format");
        }
        
        return new ValidationResult(true, "Valid Bitcoin address");
    }
    
    /**
     * Sanitizes user input to prevent injection attacks
     */
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        
        // Remove dangerous characters
        String sanitized = input.trim()
            .replaceAll("[<>\"'&]", "")  // Remove HTML/XML chars
            .replaceAll("[;|&$`]", "")   // Remove shell injection chars
            .replaceAll("\\s+", " ");    // Normalize whitespace
        
        return sanitized;
    }
    
    /**
     * Validates numeric input (balance, hash rate, etc.)
     */
    public static ValidationResult validateNumericInput(String input, double min, double max) {
        if (input == null || input.trim().isEmpty()) {
            return new ValidationResult(false, "Numeric input cannot be empty");
        }
        
        String cleanInput = sanitizeInput(input);
        
        if (!NUMERIC_PATTERN.matcher(cleanInput).matches()) {
            return new ValidationResult(false, "Input must be numeric");
        }
        
        try {
            double value = Double.parseDouble(cleanInput);
            if (value < min || value > max) {
                return new ValidationResult(false, 
                    String.format("Value must be between %.8f and %.8f", min, max));
            }
            return new ValidationResult(true, "Valid numeric input");
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Invalid number format");
        }
    }
    
    /**
     * Validates and corrects mining parameters
     */
    public static MiningParameters validateMiningParameters(double balance, double hashRate) {
        MiningParameters params = new MiningParameters();
        
        // Validate balance
        if (balance < 0) {
            params.balance = 0.0;
            params.errors.add("Balance cannot be negative - corrected to 0.0");
        } else if (balance > 21000000) { // Max Bitcoin supply
            params.balance = 21000000.0;
            params.errors.add("Balance exceeds max Bitcoin supply - corrected to 21M");
        } else {
            params.balance = balance;
        }
        
        // Validate hash rate
        if (hashRate < 0) {
            params.hashRate = 0.0;
            params.errors.add("Hash rate cannot be negative - corrected to 0.0");
        } else if (hashRate > 1000000) { // Reasonable mobile limit
            params.hashRate = 1000000.0;
            params.errors.add("Hash rate too high for mobile - corrected to 1M MH/s");
        } else {
            params.hashRate = hashRate;
        }
        
        return params;
    }
    
    /**
     * Prevents common Android parser errors
     */
    public static void validateAndroidCompatibility() {
        try {
            // Check Android version compatibility
            int sdkVersion = android.os.Build.VERSION.SDK_INT;
            if (sdkVersion < 21) {
                Log.w(TAG, "Android version below minimum requirement (API 21)");
            }
            
            // Validate memory usage
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            
            if (usedMemory > maxMemory * 0.8) {
                Log.w(TAG, "High memory usage detected - consider optimization");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Android compatibility check failed", e);
        }
    }
    
    /**
     * Fixes common string formatting issues
     */
    public static String fixStringFormatting(String input) {
        if (input == null) return "";
        
        return input
            .replaceAll("\\u0000", "")     // Remove null characters
            .replaceAll("\\p{Cntrl}", "")  // Remove control characters
            .replaceAll("\\s+", " ")       // Normalize whitespace
            .trim();
    }
    
    /**
     * Validates SharedPreferences keys and values
     */
    public static ValidationResult validatePreferencesData(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            return new ValidationResult(false, "Preference key cannot be empty");
        }
        
        if (value == null) {
            return new ValidationResult(false, "Preference value cannot be null");
        }
        
        // Sanitize key and value
        String cleanKey = sanitizeInput(key);
        String cleanValue = sanitizeInput(value);
        
        if (cleanKey.length() > 100) {
            return new ValidationResult(false, "Preference key too long");
        }
        
        if (cleanValue.length() > 1000) {
            return new ValidationResult(false, "Preference value too long");
        }
        
        return new ValidationResult(true, "Valid preference data");
    }
    
    /**
     * Prevents Intent data corruption
     */
    public static String validateIntentData(String data) {
        if (data == null) return "";
        
        // Remove potentially dangerous characters for Intent extras
        return data
            .replaceAll("[\\x00-\\x1F\\x7F]", "")  // Remove control chars
            .replaceAll("[<>\"']", "")              // Remove HTML chars
            .substring(0, Math.min(data.length(), 500)); // Limit length
    }
    
    /**
     * Validation Result class
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        
        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }
    
    /**
     * Mining Parameters class with error correction
     */
    public static class MiningParameters {
        public double balance = 0.0;
        public double hashRate = 0.0;
        public List<String> errors = new ArrayList<>();
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public String getErrorSummary() {
            if (errors.isEmpty()) return "No errors";
            return String.join("; ", errors);
        }
    }
}

