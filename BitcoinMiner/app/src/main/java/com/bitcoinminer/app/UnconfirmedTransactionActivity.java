package com.bitcoinminer.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Unconfirmed Transaction Recovery Activity
 * Handles dormant fund recovery and transaction acceleration
 */
public class UnconfirmedTransactionActivity extends Activity implements UnconfirmedTransactionManager.UnconfirmedTransactionListener {
    
    private EditText addressInput;
    private EditText txHashInput;
    private Button scanButton;
    private Button recoverButton;
    private Button accelerateButton;
    private Button calculateFeesButton;
    private ProgressBar progressBar;
    private TextView progressText;
    private ScrollView resultsScrollView;
    private LinearLayout resultsContainer;
    private TextView statusText;
    
    private UnconfirmedTransactionManager transactionManager;
    private DecimalFormat btcFormat;
    private SimpleDateFormat dateFormat;
    private List<UnconfirmedTransactionManager.DormantFund> foundDormantFunds;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unconfirmed_transaction);
        
        initializeViews();
        initializeManager();
        setupClickListeners();
    }
    
    private void initializeViews() {
        addressInput = findViewById(R.id.address_input);
        txHashInput = findViewById(R.id.tx_hash_input);
        scanButton = findViewById(R.id.scan_button);
        recoverButton = findViewById(R.id.recover_button);
        accelerateButton = findViewById(R.id.accelerate_button);
        calculateFeesButton = findViewById(R.id.calculate_fees_button);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        resultsScrollView = findViewById(R.id.results_scroll_view);
        resultsContainer = findViewById(R.id.results_container);
        statusText = findViewById(R.id.status_text);
    }
    
    private void initializeManager() {
        transactionManager = new UnconfirmedTransactionManager(this);
        btcFormat = new DecimalFormat("0.00000000");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        // Initialize status
        statusText.setText("🔍 Ready to scan for dormant funds and accelerate transactions");
    }
    
    private void setupClickListeners() {
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanForDormantFunds();
            }
        });
        
        recoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recoverDormantFunds();
            }
        });
        
        accelerateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accelerateTransaction();
            }
        });
        
        calculateFeesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateNetworkFees();
            }
        });
    }
    
    private void scanForDormantFunds() {
        String address = addressInput.getText().toString().trim();
        
        if (address.isEmpty()) {
            Toast.makeText(this, "❌ Please enter a Bitcoin address to scan", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgress(true);
        clearResults();
        statusText.setText("🔍 Scanning blockchain for dormant funds...");
        
        transactionManager.scanForDormantFunds(address, this);
    }
    
    private void recoverDormantFunds() {
        String address = addressInput.getText().toString().trim();
        
        if (address.isEmpty()) {
            Toast.makeText(this, "❌ Please enter a Bitcoin address for recovery", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (foundDormantFunds == null || foundDormantFunds.isEmpty()) {
            Toast.makeText(this, "❌ Please scan for dormant funds first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgress(true);
        statusText.setText("🔐 Initiating dormant fund recovery...");
        
        // Use the same address as both source and target for simplicity
        transactionManager.recoverDormantFunds(address, address, this);
    }
    
    private void accelerateTransaction() {
        String txHash = txHashInput.getText().toString().trim();
        
        if (txHash.isEmpty()) {
            Toast.makeText(this, "❌ Please enter a transaction hash to accelerate", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (txHash.length() != 64) {
            Toast.makeText(this, "❌ Invalid transaction hash format (must be 64 characters)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgress(true);
        statusText.setText("🚀 Accelerating transaction confirmation...");
        
        transactionManager.accelerateTransaction(txHash, this);
    }
    
    private void calculateNetworkFees() {
        String address = addressInput.getText().toString().trim();
        
        if (address.isEmpty()) {
            Toast.makeText(this, "❌ Please enter a Bitcoin address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgress(true);
        statusText.setText("💰 Calculating network and gas fees...");
        
        // Calculate fees for a typical transaction amount
        double estimatedAmount = 0.01; // 0.01 BTC
        transactionManager.calculateNetworkFees(estimatedAmount, this);
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressText.setVisibility(show ? View.VISIBLE : View.GONE);
        
        scanButton.setEnabled(!show);
        recoverButton.setEnabled(!show);
        accelerateButton.setEnabled(!show);
        calculateFeesButton.setEnabled(!show);
    }
    
    private void clearResults() {
        resultsContainer.removeAllViews();
        resultsScrollView.setVisibility(View.GONE);
    }
    
    private void addResultSection(String title, String content, boolean copyable) {
        // Create section container
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(16, 16, 16, 16);
        section.setBackgroundColor(0xFF2d2d2d);
        
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        sectionParams.setMargins(0, 0, 0, 16);
        section.setLayoutParams(sectionParams);
        
        // Title
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(0xFFFFD700);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 8);
        section.addView(titleView);
        
        // Content
        TextView contentView = new TextView(this);
        contentView.setText(content);
        contentView.setTextSize(14);
        contentView.setTextColor(0xFFFFFFFF);
        contentView.setTextIsSelectable(true);
        section.addView(contentView);
        
        // Copy button for copyable content
        if (copyable) {
            Button copyButton = new Button(this);
            copyButton.setText("📋 Copy");
            copyButton.setTextSize(12);
            copyButton.setPadding(16, 8, 16, 8);
            copyButton.setBackgroundColor(0xFF4CAF50);
            copyButton.setTextColor(0xFFFFFFFF);
            
            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            copyParams.setMargins(0, 8, 0, 0);
            copyButton.setLayoutParams(copyParams);
            
            final String copyContent = content;
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyToClipboard(copyContent);
                }
            });
            
            section.addView(copyButton);
        }
        
        resultsContainer.addView(section);
    }
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Transaction Data", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "📋 Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    // UnconfirmedTransactionListener implementation
    @Override
    public void onDormantFundsFound(List<UnconfirmedTransactionManager.DormantFund> dormantFunds) {
        showProgress(false);
        foundDormantFunds = dormantFunds;
        
        if (dormantFunds.isEmpty()) {
            statusText.setText("✅ Scan complete - No dormant funds found");
            addResultSection("🔍 Scan Results", 
                "No dormant or recoverable funds found at this address.\n" +
                "This address appears to be clean with no stuck transactions.", false);
        } else {
            statusText.setText("💰 Found " + dormantFunds.size() + " recoverable dormant funds!");
            
            StringBuilder fundsInfo = new StringBuilder();
            double totalRecoverable = 0;
            
            for (UnconfirmedTransactionManager.DormantFund fund : dormantFunds) {
                double btcAmount = fund.amount / 100000000.0;
                totalRecoverable += btcAmount;
                
                fundsInfo.append("• Transaction: ").append(fund.txid.substring(0, 16)).append("...\n");
                fundsInfo.append("  Amount: ").append(btcFormat.format(btcAmount)).append(" BTC\n");
                fundsInfo.append("  Status: ").append(fund.confirmed ? "✅ Confirmed" : "⏳ Unconfirmed").append("\n");
                fundsInfo.append("  Dormant Days: ").append(fund.dormantDays).append("\n");
                fundsInfo.append("  Recoverable: ").append(fund.recoverable ? "✅ Yes" : "❌ No").append("\n\n");
            }
            
            addResultSection("💰 Dormant Funds Found (" + dormantFunds.size() + ")",
                "Total Recoverable: " + btcFormat.format(totalRecoverable) + " BTC\n\n" + fundsInfo.toString(), true);
            
            // Enable recovery button
            recoverButton.setEnabled(true);
            Toast.makeText(this, "💰 Found " + btcFormat.format(totalRecoverable) + " BTC in recoverable funds!", Toast.LENGTH_LONG).show();
        }
        
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onFundsRecovered(UnconfirmedTransactionManager.RecoveryResult result) {
        showProgress(false);
        
        if (result.success) {
            statusText.setText("✅ Fund recovery successful!");
            
            addResultSection("✅ Recovery Successful",
                "Recovered Amount: " + btcFormat.format(result.recoveredAmount) + " BTC\n" +
                "Source Address: " + result.sourceAddress + "\n" +
                "Target Address: " + result.targetAddress + "\n" +
                "Recovery Transaction: " + result.recoveryTxid + "\n" +
                "Network Fees Covered: " + (result.networkFeesCovered ? "✅ Yes" : "❌ No") + "\n" +
                "Gas Fees Covered: " + (result.gasFeesCovered ? "✅ Yes" : "❌ No") + "\n" +
                "Timestamp: " + dateFormat.format(new Date(result.timestamp * 1000)) + "\n\n" +
                result.message, true);
            
            Toast.makeText(this, "✅ Successfully recovered " + btcFormat.format(result.recoveredAmount) + " BTC!", Toast.LENGTH_LONG).show();
            
            // Initialize charge system to cover fees
            transactionManager.initializeChargeSystem(result.recoveredAmount, this);
            
        } else {
            statusText.setText("❌ Fund recovery failed");
            addResultSection("❌ Recovery Failed", result.message, false);
        }
        
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onTransactionAccelerated(UnconfirmedTransactionManager.AccelerationResult result) {
        showProgress(false);
        
        if (result.accelerated) {
            statusText.setText("🚀 Transaction acceleration successful!");
            
            addResultSection("🚀 Acceleration Successful",
                "Original Transaction: " + result.originalTxid + "\n" +
                "Priority Level: " + result.priorityLevel + "\n" +
                "Network Nodes: " + result.networkNodes + "\n" +
                "Estimated Confirmation: " + result.estimatedConfirmationTime + " minutes\n" +
                "Acceleration Fee: " + (result.accelerationFee == 0 ? "FREE" : btcFormat.format(result.accelerationFee) + " BTC") + "\n" +
                "Timestamp: " + dateFormat.format(new Date(result.timestamp * 1000)) + "\n\n" +
                result.message, true);
            
            Toast.makeText(this, "🚀 Transaction accelerated! Confirmation in ~" + result.estimatedConfirmationTime + " minutes", Toast.LENGTH_LONG).show();
            
        } else {
            statusText.setText("❌ Transaction acceleration failed");
            addResultSection("❌ Acceleration Failed", result.message, false);
        }
        
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onNetworkFeesCalculated(UnconfirmedTransactionManager.FeeEstimate feeEstimate) {
        showProgress(false);
        statusText.setText("💰 Network fees calculated");
        
        addResultSection("💰 Fee Calculation",
            "Transaction Amount: " + btcFormat.format(feeEstimate.transactionAmount) + " BTC\n" +
            "Network Fee: " + btcFormat.format(feeEstimate.networkFee) + " BTC\n" +
            "Gas Fee: " + btcFormat.format(feeEstimate.gasFee) + " BTC\n" +
            "Total Fees: " + btcFormat.format(feeEstimate.totalFees) + " BTC\n" +
            "Net Amount: " + btcFormat.format(feeEstimate.netAmount) + " BTC\n" +
            "Fees Covered by System: " + (feeEstimate.feesCoveredBySystem ? "✅ Yes" : "❌ No") + "\n" +
            "User Charge: " + (feeEstimate.userCharge == 0 ? "FREE" : btcFormat.format(feeEstimate.userCharge) + " BTC") + "\n\n" +
            "All network and gas fees are automatically covered by the system!", false);
        
        resultsScrollView.setVisibility(View.VISIBLE);
        Toast.makeText(this, "💰 All fees covered by system - No charge to user!", Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onError(String error) {
        showProgress(false);
        statusText.setText("❌ Operation failed");
        Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
        
        addResultSection("❌ Error", error, false);
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onProgress(String message, int progress) {
        progressText.setText(message);
        progressBar.setProgress(progress);
        statusText.setText(message);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any ongoing operations
    }
}

