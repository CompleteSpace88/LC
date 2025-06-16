package com.bitcoinminer.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    
    private TextView balanceText;
    private TextView hashRateText;
    private TextView statusText;
    private ProgressBar miningProgress;
    private Button startStopButton;
    private Button withdrawButton;
    private EditText walletAddressInput;
    
    private boolean isMining = false;
    private double currentBalance = 0.0;
    private double currentHashRate = 0.0;
    private Handler miningHandler;
    private Runnable miningRunnable;
    private Random random;
    private DecimalFormat btcFormat;
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeVariables();
        loadSavedData();
        setupClickListeners();
    }
    
    private void initializeViews() {
        balanceText = findViewById(R.id.balance_text);
        hashRateText = findViewById(R.id.hashrate_text);
        statusText = findViewById(R.id.status_text);
        miningProgress = findViewById(R.id.mining_progress);
        startStopButton = findViewById(R.id.start_stop_button);
        withdrawButton = findViewById(R.id.withdraw_button);
        walletAddressInput = findViewById(R.id.wallet_address_input);
    }
    
    private void initializeVariables() {
        random = new Random();
        btcFormat = new DecimalFormat("0.00000000");
        miningHandler = new Handler();
        prefs = getSharedPreferences("BitcoinMiner", MODE_PRIVATE);
        
        miningRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMining) {
                    updateMiningStats();
                    miningHandler.postDelayed(this, 1000); // Update every second
                }
            }
        };
    }
    
    private void loadSavedData() {
        try {
            // Load and validate saved balance
            String savedBalance = prefs.getString("balance", "0.0");
            CodeValidator.ValidationResult balanceValidation = 
                CodeValidator.validateNumericInput(savedBalance, 0.0, 21000000.0);
            
            if (balanceValidation.isValid) {
                currentBalance = Double.parseDouble(CodeValidator.fixStringFormatting(savedBalance));
            } else {
                currentBalance = 0.0; // Safe default
                Toast.makeText(this, "⚠️ Invalid saved balance - reset to 0.0", Toast.LENGTH_SHORT).show();
            }
            
            // Load and validate saved address
            String savedAddress = prefs.getString("wallet_address", "");
            String sanitizedAddress = CodeValidator.sanitizeInput(savedAddress);
            walletAddressInput.setText(sanitizedAddress);
            
            updateDisplay();
            
        } catch (Exception e) {
            // Fallback to safe defaults
            currentBalance = 0.0;
            walletAddressInput.setText("");
            updateDisplay();
            Toast.makeText(this, "⚠️ Data loading error - using defaults", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveData() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            
            // Validate data before saving
            String balanceStr = String.valueOf(currentBalance);
            String walletAddr = walletAddressInput.getText().toString();
            
            CodeValidator.ValidationResult balanceValidation = 
                CodeValidator.validateNumericInput(balanceStr, 0.0, 21000000.0);
            CodeValidator.ValidationResult addressValidation = 
                CodeValidator.validatePreferencesData("wallet_address", walletAddr);
            
            if (balanceValidation.isValid) {
                editor.putString("balance", CodeValidator.fixStringFormatting(balanceStr));
            } else {
                editor.putString("balance", "0.0"); // Safe default
            }
            
            if (addressValidation.isValid) {
                editor.putString("wallet_address", CodeValidator.sanitizeInput(walletAddr));
            } else {
                editor.putString("wallet_address", ""); // Safe default
            }
            
            editor.apply();
            
        } catch (Exception e) {
            // Fallback save with safe defaults
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("balance", "0.0");
            editor.putString("wallet_address", "");
            editor.apply();
            Toast.makeText(this, "⚠️ Data save error - using safe defaults", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupClickListeners() {
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMining) {
                    stopMining();
                } else {
                    startMining();
                }
            }
        });
        
        withdrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                withdrawBitcoin();
            }
        });
    }
    
    private void startMining() {
        String walletAddress = walletAddressInput.getText().toString().trim();
        
        // Validate wallet address using code corrector
        CodeValidator.ValidationResult addressValidation = CodeValidator.validateBitcoinAddress(walletAddress);
        if (!addressValidation.isValid) {
            Toast.makeText(this, "❌ " + addressValidation.message, Toast.LENGTH_LONG).show();
            return;
        }
        
        // Validate Android compatibility
        CodeValidator.validateAndroidCompatibility();
        
        // Sanitize wallet address
        String sanitizedAddress = CodeValidator.sanitizeInput(walletAddress);
        walletAddressInput.setText(sanitizedAddress);
        
        isMining = true;
        startStopButton.setText("STOP MINING");
        startStopButton.setBackgroundColor(0xFFFF4444);
        statusText.setText("Status: MINING ACTIVE ✅");
        statusText.setTextColor(0xFF00FF00);
        
        // Start mining service with validated data
        Intent serviceIntent = new Intent(this, MiningService.class);
        serviceIntent.putExtra("wallet_address", CodeValidator.validateIntentData(sanitizedAddress));
        startService(serviceIntent);
        
        miningHandler.post(miningRunnable);
        Toast.makeText(this, "✅ Bitcoin mining started with validated address!", Toast.LENGTH_SHORT).show();
    }
    
    private void stopMining() {
        isMining = false;
        startStopButton.setText("START MINING");
        startStopButton.setBackgroundColor(0xFF4CAF50);
        statusText.setText("Status: MINING STOPPED");
        statusText.setTextColor(0xFFFF4444);
        currentHashRate = 0.0;
        
        // Stop mining service
        Intent serviceIntent = new Intent(this, MiningService.class);
        stopService(serviceIntent);
        
        updateDisplay();
        saveData();
        Toast.makeText(this, "Mining stopped. Balance saved.", Toast.LENGTH_SHORT).show();
    }
    
    private void updateMiningStats() {
        try {
            // Simulate realistic mining with very small increments
            double baseIncrement = 0.00000001; // 1 satoshi
            double randomMultiplier = 0.1 + (random.nextDouble() * 0.9); // 0.1 to 1.0
            double increment = baseIncrement * randomMultiplier;
            
            // Validate and correct mining parameters
            CodeValidator.MiningParameters params = CodeValidator.validateMiningParameters(
                currentBalance + increment, 
                50.0 + (random.nextDouble() * 100.0)
            );
            
            // Apply corrected values
            currentBalance = params.balance;
            currentHashRate = params.hashRate;
            
            // Show any correction messages
            if (params.hasErrors()) {
                Toast.makeText(this, "⚠️ " + params.getErrorSummary(), Toast.LENGTH_SHORT).show();
            }
            
            // Update progress bar with bounds checking
            int progress = Math.max(0, Math.min(100, (int) ((currentBalance * 100000000) % 100)));
            miningProgress.setProgress(progress);
            
            updateDisplay();
            saveData();
            
        } catch (Exception e) {
            // Error handling to prevent crashes
            Toast.makeText(this, "⚠️ Mining calculation error - resetting", Toast.LENGTH_SHORT).show();
            currentBalance = Math.max(0, currentBalance); // Ensure non-negative
            currentHashRate = 0.0;
            updateDisplay();
        }
    }
    
    private void updateDisplay() {
        try {
            // Format and validate display strings
            String balanceStr = btcFormat.format(Math.max(0, currentBalance));
            String hashRateStr = String.format("%.2f", Math.max(0, currentHashRate));
            
            // Apply string formatting fixes
            String safeBalanceStr = CodeValidator.fixStringFormatting(balanceStr);
            String safeHashRateStr = CodeValidator.fixStringFormatting(hashRateStr);
            
            balanceText.setText("Balance: " + safeBalanceStr + " BTC");
            hashRateText.setText("Hash Rate: " + safeHashRateStr + " MH/s");
            
            if (isMining) {
                miningProgress.setVisibility(View.VISIBLE);
            } else {
                miningProgress.setVisibility(View.GONE);
            }
            
        } catch (Exception e) {
            // Fallback display with safe values
            balanceText.setText("Balance: 0.00000000 BTC");
            hashRateText.setText("Hash Rate: 0.00 MH/s");
            miningProgress.setVisibility(View.GONE);
        }
    }
    
    private void withdrawBitcoin() {
        String walletAddress = walletAddressInput.getText().toString().trim();
        
        // Comprehensive validation using code corrector
        CodeValidator.ValidationResult addressValidation = CodeValidator.validateBitcoinAddress(walletAddress);
        if (!addressValidation.isValid) {
            Toast.makeText(this, "❌ " + addressValidation.message, Toast.LENGTH_LONG).show();
            return;
        }
        
        // Validate balance
        CodeValidator.ValidationResult balanceValidation = 
            CodeValidator.validateNumericInput(String.valueOf(currentBalance), 0.00000001, 21000000.0);
        if (!balanceValidation.isValid || currentBalance <= 0.0) {
            Toast.makeText(this, "❌ No Bitcoin available to withdraw", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Sanitize address for display
            String sanitizedAddress = CodeValidator.sanitizeInput(walletAddress);
            String formattedBalance = btcFormat.format(currentBalance);
            
            // Validate formatted strings
            String safeBalance = CodeValidator.fixStringFormatting(formattedBalance);
            String safeAddress = CodeValidator.fixStringFormatting(sanitizedAddress);
            
            // Simulate withdrawal process with validated data
            String message = "✅ Withdrawal Request:\n" +
                            "Amount: " + safeBalance + " BTC\n" +
                            "To Address: " + safeAddress + "\n" +
                            "Status: Processing...";
            
            Toast.makeText(this, "✅ Withdrawal initiated for " + safeBalance + " BTC", Toast.LENGTH_LONG).show();
            
            // Reset balance after withdrawal with validation
            currentBalance = 0.0;
            updateDisplay();
            saveData();
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Withdrawal processing error - please try again", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMining) {
            stopMining();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveData();
    }
}
