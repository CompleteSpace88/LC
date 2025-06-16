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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Blockchain Explorer Activity
 * Provides interface for blockchain data lookup and exploration
 */
public class BlockchainExplorerActivity extends Activity implements BlockchainExplorer.BlockchainExplorerListener {
    
    private EditText searchInput;
    private Button searchButton;
    private Button priceButton;
    private Button networkStatsButton;
    private ProgressBar loadingProgress;
    private ScrollView resultsScrollView;
    private LinearLayout resultsContainer;
    private TextView currentPriceText;
    
    private BlockchainExplorer blockchainExplorer;
    private SimpleDateFormat dateFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blockchain_explorer);
        
        initializeViews();
        initializeExplorer();
        setupClickListeners();
    }
    
    private void initializeViews() {
        searchInput = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);
        priceButton = findViewById(R.id.price_button);
        networkStatsButton = findViewById(R.id.network_stats_button);
        loadingProgress = findViewById(R.id.loading_progress);
        resultsScrollView = findViewById(R.id.results_scroll_view);
        resultsContainer = findViewById(R.id.results_container);
        currentPriceText = findViewById(R.id.current_price_text);
    }
    
    private void initializeExplorer() {
        blockchainExplorer = new BlockchainExplorer(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        // Load current price on startup
        loadCurrentPrice();
    }
    
    private void setupClickListeners() {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });
        
        priceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCurrentPrice();
            }
        });
        
        networkStatsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNetworkStats();
            }
        });
    }
    
    private void performSearch() {
        String query = searchInput.getText().toString().trim();
        
        if (query.isEmpty()) {
            Toast.makeText(this, "❌ Please enter an address or transaction hash", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        clearResults();
        
        // Determine search type based on input format
        if (isValidBitcoinAddress(query)) {
            // Search for address
            blockchainExplorer.lookupAddress(query, this);
        } else if (isValidTransactionHash(query)) {
            // Search for transaction
            blockchainExplorer.lookupTransaction(query, this);
        } else if (isValidBlockHash(query) || isValidBlockHeight(query)) {
            // Search for block
            blockchainExplorer.lookupBlock(query, this);
        } else {
            showLoading(false);
            Toast.makeText(this, "❌ Invalid format. Enter a Bitcoin address, transaction hash, or block identifier", Toast.LENGTH_LONG).show();
        }
    }
    
    private void loadCurrentPrice() {
        showLoading(true);
        blockchainExplorer.getCurrentPrice(this);
    }
    
    private void loadNetworkStats() {
        showLoading(true);
        clearResults();
        blockchainExplorer.getNetworkStats(this);
    }
    
    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!show);
        priceButton.setEnabled(!show);
        networkStatsButton.setEnabled(!show);
    }
    
    private void clearResults() {
        resultsContainer.removeAllViews();
    }
    
    private void addResultSection(String title, String content, boolean copyable) {
        // Create section container
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(16, 16, 16, 16);
        section.setBackground(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
        
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
        titleView.setTextColor(0xFF2196F3);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 8);
        section.addView(titleView);
        
        // Content
        TextView contentView = new TextView(this);
        contentView.setText(content);
        contentView.setTextSize(14);
        contentView.setTextColor(0xFF333333);
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
        ClipData clip = ClipData.newPlainText("Blockchain Data", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "📋 Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    // Validation methods
    private boolean isValidBitcoinAddress(String address) {
        CodeValidator.ValidationResult result = CodeValidator.validateBitcoinAddress(address);
        return result.isValid;
    }
    
    private boolean isValidTransactionHash(String hash) {
        return hash != null && hash.length() == 64 && hash.matches("[a-fA-F0-9]+");
    }
    
    private boolean isValidBlockHash(String hash) {
        return hash != null && hash.length() == 64 && hash.matches("[a-fA-F0-9]+");
    }
    
    private boolean isValidBlockHeight(String height) {
        try {
            int h = Integer.parseInt(height);
            return h >= 0 && h <= 1000000; // Reasonable block height range
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // BlockchainExplorerListener implementation
    @Override
    public void onAddressInfoReceived(BlockchainExplorer.AddressInfo addressInfo) {
        showLoading(false);
        
        // Address Information
        addResultSection("📍 Address Information", 
            "Address: " + addressInfo.address + "\n" +
            "Balance: " + blockchainExplorer.formatBTC(addressInfo.balance) + " BTC\n" +
            "Total Received: " + blockchainExplorer.formatBTC(addressInfo.totalReceived) + " BTC\n" +
            "Total Sent: " + blockchainExplorer.formatBTC(addressInfo.totalSent) + " BTC\n" +
            "Transaction Count: " + addressInfo.transactionCount, true);
        
        // Recent Transactions
        if (addressInfo.recentTransactions != null && !addressInfo.recentTransactions.isEmpty()) {
            StringBuilder txList = new StringBuilder();
            for (BlockchainExplorer.TransactionSummary tx : addressInfo.recentTransactions) {
                txList.append("• ").append(tx.txid.substring(0, 16)).append("...\n");
                txList.append("  Confirmations: ").append(tx.confirmations).append("\n");
                if (tx.timestamp > 0) {
                    txList.append("  Time: ").append(blockchainExplorer.formatTimestamp(tx.timestamp)).append("\n");
                }
                txList.append("\n");
            }
            
            addResultSection("📋 Recent Transactions", txList.toString(), false);
        }
        
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onTransactionInfoReceived(BlockchainExplorer.TransactionInfo transactionInfo) {
        showLoading(false);
        
        // Transaction Information
        String confirmationStatus = transactionInfo.confirmed ? "✅ Confirmed" : "⏳ Unconfirmed";
        
        addResultSection("🔗 Transaction Information",
            "Transaction ID: " + transactionInfo.txid + "\n" +
            "Status: " + confirmationStatus + "\n" +
            "Confirmations: " + transactionInfo.confirmations + "\n" +
            "Block Height: " + (transactionInfo.blockHeight > 0 ? transactionInfo.blockHeight : "Unconfirmed") + "\n" +
            "Block Hash: " + (transactionInfo.blockHash.isEmpty() ? "Unconfirmed" : transactionInfo.blockHash.substring(0, 16) + "...") + "\n" +
            "Size: " + transactionInfo.size + " bytes\n" +
            "Fee: " + blockchainExplorer.formatBTC(transactionInfo.fee) + " BTC\n" +
            "Input Value: " + blockchainExplorer.formatBTC(transactionInfo.inputValue) + " BTC\n" +
            "Output Value: " + blockchainExplorer.formatBTC(transactionInfo.outputValue) + " BTC\n" +
            "Time: " + (transactionInfo.timestamp > 0 ? blockchainExplorer.formatTimestamp(transactionInfo.timestamp) : "Pending"), true);
        
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onPriceInfoReceived(BlockchainExplorer.PriceInfo priceInfo) {
        showLoading(false);
        
        String priceText = "💰 Bitcoin Price: " + blockchainExplorer.formatPrice(priceInfo.priceUSD) + " USD";
        currentPriceText.setText(priceText);
        currentPriceText.setVisibility(View.VISIBLE);
        
        // Also add to results if this was a manual price check
        if (resultsContainer.getChildCount() == 0) {
            addResultSection("💰 Current Bitcoin Price",
                "Price: " + blockchainExplorer.formatPrice(priceInfo.priceUSD) + " USD\n" +
                "Currency: " + priceInfo.currency + "\n" +
                "Last Updated: " + priceInfo.lastUpdated, false);
            
            resultsScrollView.setVisibility(View.VISIBLE);
        }
        
        Toast.makeText(this, "💰 Price updated: " + blockchainExplorer.formatPrice(priceInfo.priceUSD), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBlockInfoReceived(BlockchainExplorer.BlockInfo blockInfo) {
        showLoading(false);
        
        addResultSection("🧱 Block Information",
            "Block Hash: " + blockInfo.hash + "\n" +
            "Block Height: " + blockInfo.height + "\n" +
            "Timestamp: " + blockchainExplorer.formatTimestamp(blockInfo.timestamp) + "\n" +
            "Transaction Count: " + blockInfo.transactionCount + "\n" +
            "Block Size: " + blockInfo.size + " bytes\n" +
            "Block Weight: " + blockInfo.weight + "\n" +
            "Difficulty: " + String.format("%.2e", blockInfo.difficulty) + "\n" +
            "Nonce: " + blockInfo.nonce + "\n" +
            "Previous Block: " + (blockInfo.previousBlockHash.isEmpty() ? "Genesis Block" : blockInfo.previousBlockHash.substring(0, 16) + "...") + "\n" +
            "Merkle Root: " + (blockInfo.merkleRoot.isEmpty() ? "N/A" : blockInfo.merkleRoot.substring(0, 16) + "..."), true);
        
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onError(String error) {
        showLoading(false);
        Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
        
        // Show error in results
        addResultSection("❌ Error", error, false);
        resultsScrollView.setVisibility(View.VISIBLE);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any ongoing network requests if needed
    }
}

