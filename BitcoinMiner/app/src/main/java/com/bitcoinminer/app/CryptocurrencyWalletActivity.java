package com.bitcoinminer.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Comprehensive Cryptocurrency Wallet Activity
 * Multi-currency wallet with advanced security features
 */
public class CryptocurrencyWalletActivity extends Activity implements CryptocurrencyWallet.WalletListener {
    
    // UI Components
    private TextView securityStatusText;
    private TextView totalBalanceText;
    private Spinner currencySpinner;
    private TextView currentBalanceText;
    private TextView currentPriceText;
    private EditText sendAddressInput;
    private EditText sendAmountInput;
    private EditText privateKeyInput;
    private Button sendButton;
    private Button receiveButton;
    private Button refreshButton;
    private Button swapButton;
    private Button exportHistoryButton;
    private Spinner swapFromSpinner;
    private Spinner swapToSpinner;
    private EditText swapAmountInput;
    private ProgressBar progressBar;
    private TextView progressText;
    private ScrollView resultsScrollView;
    private LinearLayout resultsContainer;
    private LinearLayout balancesContainer;
    private LinearLayout pricesContainer;
    private ImageView qrCodeImageView;
    private TextView receiveAddressText;
    
    // Core components
    private CryptocurrencyWallet wallet;
    private SecurityManager securityManager;
    private DecimalFormat cryptoFormat;
    private DecimalFormat priceFormat;
    private SimpleDateFormat dateFormat;
    private Map<String, CryptocurrencyWallet.CryptocurrencyInfo> supportedCoins;
    private Map<String, Double> currentPrices;
    private Map<String, Double> currentBalances;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cryptocurrency_wallet);
        
        initializeComponents();
        initializeUI();
        setupClickListeners();
        performInitialSecurityCheck();
        loadWalletData();
    }
    
    private void initializeComponents() {
        wallet = new CryptocurrencyWallet(this);
        securityManager = new SecurityManager(this);
        cryptoFormat = new DecimalFormat("0.00000000");
        priceFormat = new DecimalFormat("$#,##0.00");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        supportedCoins = wallet.getSupportedCoins();
        currentPrices = new java.util.HashMap<>();
        currentBalances = new java.util.HashMap<>();
    }
    
    private void initializeUI() {
        // Find all UI components
        securityStatusText = findViewById(R.id.security_status_text);
        totalBalanceText = findViewById(R.id.total_balance_text);
        currencySpinner = findViewById(R.id.currency_spinner);
        currentBalanceText = findViewById(R.id.current_balance_text);
        currentPriceText = findViewById(R.id.current_price_text);
        sendAddressInput = findViewById(R.id.send_address_input);
        sendAmountInput = findViewById(R.id.send_amount_input);
        privateKeyInput = findViewById(R.id.private_key_input);
        sendButton = findViewById(R.id.send_button);
        receiveButton = findViewById(R.id.receive_button);
        refreshButton = findViewById(R.id.refresh_button);
        swapButton = findViewById(R.id.swap_button);
        exportHistoryButton = findViewById(R.id.export_history_button);
        swapFromSpinner = findViewById(R.id.swap_from_spinner);
        swapToSpinner = findViewById(R.id.swap_to_spinner);
        swapAmountInput = findViewById(R.id.swap_amount_input);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        resultsScrollView = findViewById(R.id.results_scroll_view);
        resultsContainer = findViewById(R.id.results_container);
        balancesContainer = findViewById(R.id.balances_container);
        pricesContainer = findViewById(R.id.prices_container);
        qrCodeImageView = findViewById(R.id.qr_code_image);
        receiveAddressText = findViewById(R.id.receive_address_text);
        
        // Setup spinners
        setupCurrencySpinners();
        
        // Initialize security status
        updateSecurityStatus();
    }
    
    private void setupCurrencySpinners() {
        List<String> currencies = new ArrayList<>(supportedCoins.keySet());
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        currencySpinner.setAdapter(adapter);
        swapFromSpinner.setAdapter(adapter);
        swapToSpinner.setAdapter(adapter);
    }
    
    private void setupClickListeners() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCryptocurrency();
            }
        });
        
        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateReceiveAddress();
            }
        });
        
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshWalletData();
            }
        });
        
        swapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSwap();
            }
        });
        
        exportHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportTransactionHistory();
            }
        });
    }
    
    private void performInitialSecurityCheck() {
        showProgress(true, "🔒 Performing security scan...");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean securityStatus = securityManager.performComprehensiveSecurityCheck();
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false, "");
                        updateSecurityStatus();
                        
                        if (!securityStatus) {
                            Toast.makeText(CryptocurrencyWalletActivity.this, 
                                "⚠️ Security threats detected. Wallet protection active.", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(CryptocurrencyWalletActivity.this, 
                                "✅ Security scan complete. Wallet is secure.", 
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }
    
    private void updateSecurityStatus() {
        SecurityManager.SecurityStatus status = securityManager.getSecurityStatus();
        
        String statusText = "🛡️ Security Level: " + status.overallSecurityLevel + "%";
        if (status.overallSecurityLevel >= 90) {
            statusText += " (EXCELLENT)";
            securityStatusText.setTextColor(0xFF00FF00); // Green
        } else if (status.overallSecurityLevel >= 70) {
            statusText += " (GOOD)";
            securityStatusText.setTextColor(0xFFFFFF00); // Yellow
        } else {
            statusText += " (NEEDS ATTENTION)";
            securityStatusText.setTextColor(0xFFFF0000); // Red
        }
        
        securityStatusText.setText(statusText);
    }
    
    private void loadWalletData() {
        showProgress(true, "📊 Loading wallet data...");
        
        // Load all balances
        wallet.getAllBalances(this);
        
        // Load current prices
        wallet.getCryptocurrencyPrices(this);
    }
    
    private void refreshWalletData() {
        showProgress(true, "🔄 Refreshing wallet data...");
        
        // Perform security check first
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean securityStatus = securityManager.performSecurityScan();
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (securityStatus) {
                            wallet.getAllBalances(CryptocurrencyWalletActivity.this);
                            wallet.getCryptocurrencyPrices(CryptocurrencyWalletActivity.this);
                        } else {
                            showProgress(false, "");
                            Toast.makeText(CryptocurrencyWalletActivity.this, 
                                "❌ Security threat detected. Refresh blocked.", 
                                Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }
    
    private void sendCryptocurrency() {
        String currency = currencySpinner.getSelectedItem().toString();
        String address = sendAddressInput.getText().toString().trim();
        String amountStr = sendAmountInput.getText().toString().trim();
        String privateKey = privateKeyInput.getText().toString().trim();
        
        if (address.isEmpty()) {
            Toast.makeText(this, "❌ Please enter recipient address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "❌ Please enter amount to send", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (privateKey.isEmpty()) {
            Toast.makeText(this, "❌ Please enter private key for authorization", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "❌ Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
            
            showProgress(true, "🔒 Performing security verification...");
            wallet.sendCryptocurrency(currency, address, amount, privateKey, this);
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "❌ Invalid amount format", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void generateReceiveAddress() {
        String currency = currencySpinner.getSelectedItem().toString();
        
        showProgress(true, "🔐 Generating secure address...");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String address = wallet.generateReceivingAddress(currency);
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false, "");
                        receiveAddressText.setText(address);
                        generateQRCode(address);
                        
                        addResultSection("📥 Receive " + currency,
                            "Address: " + address + "\n" +
                            "Currency: " + supportedCoins.get(currency).name + "\n" +
                            "Generated: " + dateFormat.format(new Date()) + "\n\n" +
                            "Share this address to receive " + currency + " payments.", true);
                        
                        resultsScrollView.setVisibility(View.VISIBLE);
                        Toast.makeText(CryptocurrencyWalletActivity.this, 
                            "📥 Receive address generated for " + currency, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
    
    private void performSwap() {
        String fromCurrency = swapFromSpinner.getSelectedItem().toString();
        String toCurrency = swapToSpinner.getSelectedItem().toString();
        String amountStr = swapAmountInput.getText().toString().trim();
        
        if (fromCurrency.equals(toCurrency)) {
            Toast.makeText(this, "❌ Cannot swap same currency", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "❌ Please enter amount to swap", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "❌ Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
            
            showProgress(true, "🔒 Security verification...");
            wallet.swapCryptocurrency(fromCurrency, toCurrency, amount, this);
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "❌ Invalid amount format", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void exportTransactionHistory() {
        showProgress(true, "📋 Exporting transaction history...");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String history = wallet.exportTransactionHistory();
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false, "");
                        
                        addResultSection("📋 Transaction History Export",
                            history, true);
                        
                        resultsScrollView.setVisibility(View.VISIBLE);
                        Toast.makeText(CryptocurrencyWalletActivity.this, 
                            "📋 Transaction history exported", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
    
    private void generateQRCode(String address) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(address, BarcodeFormat.QR_CODE, 200, 200);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            qrCodeImageView.setImageBitmap(bitmap);
            qrCodeImageView.setVisibility(View.VISIBLE);
            
        } catch (WriterException e) {
            Toast.makeText(this, "❌ Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showProgress(boolean show, String message) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressText.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && !message.isEmpty()) {
            progressText.setText(message);
        }
        
        // Disable buttons during operations
        sendButton.setEnabled(!show);
        receiveButton.setEnabled(!show);
        refreshButton.setEnabled(!show);
        swapButton.setEnabled(!show);
        exportHistoryButton.setEnabled(!show);
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
        ClipData clip = ClipData.newPlainText("Wallet Data", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "📋 Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    private void updateBalanceDisplay() {
        balancesContainer.removeAllViews();
        
        double totalUSDValue = 0;
        
        for (Map.Entry<String, Double> entry : currentBalances.entrySet()) {
            String currency = entry.getKey();
            double balance = entry.getValue();
            
            LinearLayout balanceRow = new LinearLayout(this);
            balanceRow.setOrientation(LinearLayout.HORIZONTAL);
            balanceRow.setPadding(8, 4, 8, 4);
            
            TextView currencyText = new TextView(this);
            currencyText.setText(currency + ":");
            currencyText.setTextColor(0xFFFFFFFF);
            currencyText.setTextSize(14);
            currencyText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            TextView balanceText = new TextView(this);
            balanceText.setText(cryptoFormat.format(balance));
            balanceText.setTextColor(0xFF00FF00);
            balanceText.setTextSize(14);
            balanceText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            
            balanceRow.addView(currencyText);
            balanceRow.addView(balanceText);
            balancesContainer.addView(balanceRow);
            
            // Calculate USD value
            if (currentPrices.containsKey(currency)) {
                totalUSDValue += balance * currentPrices.get(currency);
            }
        }
        
        totalBalanceText.setText("Total Portfolio Value: " + priceFormat.format(totalUSDValue));
    }
    
    private void updatePriceDisplay() {
        pricesContainer.removeAllViews();
        
        for (Map.Entry<String, Double> entry : currentPrices.entrySet()) {
            String currency = entry.getKey();
            double price = entry.getValue();
            
            LinearLayout priceRow = new LinearLayout(this);
            priceRow.setOrientation(LinearLayout.HORIZONTAL);
            priceRow.setPadding(8, 4, 8, 4);
            
            TextView currencyText = new TextView(this);
            currencyText.setText(currency + ":");
            currencyText.setTextColor(0xFFFFFFFF);
            currencyText.setTextSize(14);
            currencyText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            TextView priceText = new TextView(this);
            priceText.setText(priceFormat.format(price));
            priceText.setTextColor(0xFFFFD700);
            priceText.setTextSize(14);
            priceText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            
            priceRow.addView(currencyText);
            priceRow.addView(priceText);
            pricesContainer.addView(priceRow);
        }
    }
    
    // WalletListener implementation
    @Override
    public void onBalanceUpdated(String currency, double balance) {
        currentBalances.put(currency, balance);
        updateBalanceDisplay();
        
        // Update current balance text if this is the selected currency
        if (currency.equals(currencySpinner.getSelectedItem().toString())) {
            currentBalanceText.setText("Balance: " + cryptoFormat.format(balance) + " " + currency);
        }
    }
    
    @Override
    public void onTransactionComplete(CryptocurrencyWallet.TransactionResult result) {
        showProgress(false, "");
        
        if (result.success) {
            addResultSection("✅ Transaction Sent",
                "Currency: " + result.currency + "\n" +
                "Amount: " + cryptoFormat.format(result.amount) + " " + result.currency + "\n" +
                "To Address: " + result.toAddress + "\n" +
                "From Address: " + result.fromAddress + "\n" +
                "Transaction ID: " + result.txId + "\n" +
                "Fee: " + cryptoFormat.format(result.fee) + " " + result.currency + "\n" +
                "Status: " + result.status + "\n" +
                "Timestamp: " + result.timestamp, true);
            
            Toast.makeText(this, "✅ Transaction sent successfully!", Toast.LENGTH_LONG).show();
            
            // Clear input fields
            sendAddressInput.setText("");
            sendAmountInput.setText("");
            privateKeyInput.setText("");
            
            // Refresh balances
            wallet.getAllBalances(this);
            
        } else {
            Toast.makeText(this, "❌ Transaction failed", Toast.LENGTH_LONG).show();
        }
        
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onPricesUpdated(Map<String, Double> prices) {
        currentPrices.putAll(prices);
        updatePriceDisplay();
        updateBalanceDisplay(); // Update to recalculate total value
        
        // Update current price text if currency is selected
        String selectedCurrency = currencySpinner.getSelectedItem().toString();
        if (prices.containsKey(selectedCurrency)) {
            currentPriceText.setText("Price: " + priceFormat.format(prices.get(selectedCurrency)));
        }
        
        showProgress(false, "");
        Toast.makeText(this, "💰 Prices updated", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onSwapComplete(CryptocurrencyWallet.SwapResult result) {
        showProgress(false, "");
        
        if (result.success) {
            addResultSection("🔄 Swap Completed",
                "From: " + cryptoFormat.format(result.fromAmount) + " " + result.fromCurrency + "\n" +
                "To: " + cryptoFormat.format(result.toAmount) + " " + result.toCurrency + "\n" +
                "Exchange Rate: " + cryptoFormat.format(result.exchangeRate) + "\n" +
                "Swap Fee: " + cryptoFormat.format(result.swapFee) + " " + result.fromCurrency + "\n" +
                "Swap ID: " + result.swapId + "\n" +
                "Timestamp: " + result.timestamp, true);
            
            Toast.makeText(this, "🔄 Swap completed successfully!", Toast.LENGTH_LONG).show();
            
            // Clear input field
            swapAmountInput.setText("");
            
            // Refresh balances
            wallet.getAllBalances(this);
            
        } else {
            Toast.makeText(this, "❌ Swap failed", Toast.LENGTH_LONG).show();
        }
        
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onSecurityAlert(CryptocurrencyWallet.SecurityAlert alert) {
        String alertMessage = "🚨 SECURITY ALERT: " + alert.message;
        Toast.makeText(this, alertMessage, Toast.LENGTH_LONG).show();
        
        addResultSection("🚨 Security Alert",
            "Alert Type: " + alert.alertType + "\n" +
            "Message: " + alert.message + "\n" +
            "Severity: " + alert.severity + "/5\n" +
            "Timestamp: " + alert.timestamp, false);
        
        resultsScrollView.setVisibility(View.VISIBLE);
        updateSecurityStatus();
    }
    
    @Override
    public void onError(String error) {
        showProgress(false, "");
        Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
        
        addResultSection("❌ Error", error, false);
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onProgress(String message, int progress) {
        progressText.setText(message);
        progressBar.setProgress(progress);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any ongoing operations
    }
}

