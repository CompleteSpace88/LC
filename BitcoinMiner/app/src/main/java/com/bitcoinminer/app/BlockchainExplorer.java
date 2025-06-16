package com.bitcoinminer.app;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Blockchain Explorer for Bitcoin Miner App
 * Provides real-time blockchain data, address lookup, and price information
 */
public class BlockchainExplorer {
    
    private static final String TAG = "BlockchainExplorer";
    
    // API Endpoints
    private static final String BLOCKCHAIN_API_BASE = "https://blockstream.info/api";
    private static final String PRICE_API_BASE = "https://api.coindesk.com/v1/bpi/currentprice.json";
    private static final String MEMPOOL_API_BASE = "https://mempool.space/api";
    
    private Context context;
    private DecimalFormat btcFormat;
    private DecimalFormat priceFormat;
    private SimpleDateFormat dateFormat;
    
    public interface BlockchainExplorerListener {
        void onAddressInfoReceived(AddressInfo addressInfo);
        void onTransactionInfoReceived(TransactionInfo transactionInfo);
        void onPriceInfoReceived(PriceInfo priceInfo);
        void onBlockInfoReceived(BlockInfo blockInfo);
        void onError(String error);
    }
    
    public BlockchainExplorer(Context context) {
        this.context = context.getApplicationContext();
        this.btcFormat = new DecimalFormat("0.00000000");
        this.priceFormat = new DecimalFormat("$#,##0.00");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }
    
    /**
     * Look up Bitcoin address information
     */
    public void lookupAddress(String address, BlockchainExplorerListener listener) {
        new AddressLookupTask(listener).execute(address);
    }
    
    /**
     * Look up transaction by hash ID
     */
    public void lookupTransaction(String txHash, BlockchainExplorerListener listener) {
        new TransactionLookupTask(listener).execute(txHash);
    }
    
    /**
     * Get current Bitcoin price
     */
    public void getCurrentPrice(BlockchainExplorerListener listener) {
        new PriceLookupTask(listener).execute();
    }
    
    /**
     * Get block information by hash or height
     */
    public void lookupBlock(String blockHashOrHeight, BlockchainExplorerListener listener) {
        new BlockLookupTask(listener).execute(blockHashOrHeight);
    }
    
    /**
     * Get network statistics
     */
    public void getNetworkStats(BlockchainExplorerListener listener) {
        new NetworkStatsTask(listener).execute();
    }
    
    /**
     * Address lookup task
     */
    private class AddressLookupTask extends AsyncTask<String, Void, AddressInfo> {
        private BlockchainExplorerListener listener;
        private String error;
        
        public AddressLookupTask(BlockchainExplorerListener listener) {
            this.listener = listener;
        }
        
        @Override
        protected AddressInfo doInBackground(String... addresses) {
            try {
                String address = addresses[0];
                
                // Validate address format first
                CodeValidator.ValidationResult validation = CodeValidator.validateBitcoinAddress(address);
                if (!validation.isValid) {
                    error = validation.message;
                    return null;
                }
                
                // Get address info from Blockstream API
                String url = BLOCKCHAIN_API_BASE + "/address/" + address;
                String response = makeHttpRequest(url);
                
                if (response != null) {
                    JSONObject json = new JSONObject(response);
                    
                    AddressInfo addressInfo = new AddressInfo();
                    addressInfo.address = address;
                    addressInfo.balance = json.optLong("chain_stats.funded_txo_sum", 0) - 
                                        json.optLong("chain_stats.spent_txo_sum", 0);
                    addressInfo.totalReceived = json.optLong("chain_stats.funded_txo_sum", 0);
                    addressInfo.totalSent = json.optLong("chain_stats.spent_txo_sum", 0);
                    addressInfo.transactionCount = json.optInt("chain_stats.tx_count", 0);
                    
                    // Get recent transactions
                    String txUrl = BLOCKCHAIN_API_BASE + "/address/" + address + "/txs";
                    String txResponse = makeHttpRequest(txUrl);
                    if (txResponse != null) {
                        JSONArray txArray = new JSONArray(txResponse);
                        addressInfo.recentTransactions = new ArrayList<>();
                        
                        int maxTx = Math.min(5, txArray.length()); // Get last 5 transactions
                        for (int i = 0; i < maxTx; i++) {
                            JSONObject tx = txArray.getJSONObject(i);
                            TransactionSummary txSummary = new TransactionSummary();
                            txSummary.txid = tx.getString("txid");
                            txSummary.confirmations = tx.optInt("status.block_height", 0) > 0 ? 
                                getCurrentBlockHeight() - tx.optInt("status.block_height", 0) + 1 : 0;
                            txSummary.timestamp = tx.optLong("status.block_time", 0);
                            addressInfo.recentTransactions.add(txSummary);
                        }
                    }
                    
                    return addressInfo;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error looking up address", e);
                error = "Failed to lookup address: " + e.getMessage();
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(AddressInfo result) {
            if (result != null) {
                listener.onAddressInfoReceived(result);
            } else {
                listener.onError(error != null ? error : "Failed to lookup address");
            }
        }
    }
    
    /**
     * Transaction lookup task
     */
    private class TransactionLookupTask extends AsyncTask<String, Void, TransactionInfo> {
        private BlockchainExplorerListener listener;
        private String error;
        
        public TransactionLookupTask(BlockchainExplorerListener listener) {
            this.listener = listener;
        }
        
        @Override
        protected TransactionInfo doInBackground(String... txHashes) {
            try {
                String txHash = txHashes[0];
                
                // Validate transaction hash format
                if (txHash == null || txHash.length() != 64) {
                    error = "Invalid transaction hash format";
                    return null;
                }
                
                String url = BLOCKCHAIN_API_BASE + "/tx/" + txHash;
                String response = makeHttpRequest(url);
                
                if (response != null) {
                    JSONObject json = new JSONObject(response);
                    
                    TransactionInfo txInfo = new TransactionInfo();
                    txInfo.txid = txHash;
                    txInfo.blockHash = json.optString("status.block_hash", "");
                    txInfo.blockHeight = json.optInt("status.block_height", 0);
                    txInfo.confirmations = txInfo.blockHeight > 0 ? 
                        getCurrentBlockHeight() - txInfo.blockHeight + 1 : 0;
                    txInfo.timestamp = json.optLong("status.block_time", 0);
                    txInfo.size = json.optInt("size", 0);
                    txInfo.fee = json.optLong("fee", 0);
                    txInfo.confirmed = json.optBoolean("status.confirmed", false);
                    
                    // Calculate total input and output values
                    JSONArray inputs = json.optJSONArray("vin");
                    JSONArray outputs = json.optJSONArray("vout");
                    
                    long totalInput = 0;
                    long totalOutput = 0;
                    
                    if (inputs != null) {
                        for (int i = 0; i < inputs.length(); i++) {
                            JSONObject input = inputs.getJSONObject(i);
                            totalInput += input.optJSONObject("prevout").optLong("value", 0);
                        }
                    }
                    
                    if (outputs != null) {
                        for (int i = 0; i < outputs.length(); i++) {
                            JSONObject output = outputs.getJSONObject(i);
                            totalOutput += output.optLong("value", 0);
                        }
                    }
                    
                    txInfo.inputValue = totalInput;
                    txInfo.outputValue = totalOutput;
                    
                    return txInfo;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error looking up transaction", e);
                error = "Failed to lookup transaction: " + e.getMessage();
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(TransactionInfo result) {
            if (result != null) {
                listener.onTransactionInfoReceived(result);
            } else {
                listener.onError(error != null ? error : "Failed to lookup transaction");
            }
        }
    }
    
    /**
     * Price lookup task
     */
    private class PriceLookupTask extends AsyncTask<Void, Void, PriceInfo> {
        private BlockchainExplorerListener listener;
        private String error;
        
        public PriceLookupTask(BlockchainExplorerListener listener) {
            this.listener = listener;
        }
        
        @Override
        protected PriceInfo doInBackground(Void... voids) {
            try {
                String response = makeHttpRequest(PRICE_API_BASE);
                
                if (response != null) {
                    JSONObject json = new JSONObject(response);
                    JSONObject bpi = json.getJSONObject("bpi");
                    JSONObject usd = bpi.getJSONObject("USD");
                    
                    PriceInfo priceInfo = new PriceInfo();
                    priceInfo.priceUSD = usd.getDouble("rate_float");
                    priceInfo.currency = "USD";
                    priceInfo.lastUpdated = json.getJSONObject("time").getString("updated");
                    
                    // Get additional price data from alternative source
                    try {
                        String mempoolResponse = makeHttpRequest(MEMPOOL_API_BASE + "/v1/prices");
                        if (mempoolResponse != null) {
                            JSONObject mempoolJson = new JSONObject(mempoolResponse);
                            priceInfo.priceUSD = mempoolJson.optDouble("USD", priceInfo.priceUSD);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Alternative price source failed", e);
                    }
                    
                    return priceInfo;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting price", e);
                error = "Failed to get price: " + e.getMessage();
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(PriceInfo result) {
            if (result != null) {
                listener.onPriceInfoReceived(result);
            } else {
                listener.onError(error != null ? error : "Failed to get price");
            }
        }
    }
    
    /**
     * Block lookup task
     */
    private class BlockLookupTask extends AsyncTask<String, Void, BlockInfo> {
        private BlockchainExplorerListener listener;
        private String error;
        
        public BlockLookupTask(BlockchainExplorerListener listener) {
            this.listener = listener;
        }
        
        @Override
        protected BlockInfo doInBackground(String... blockIdentifiers) {
            try {
                String blockId = blockIdentifiers[0];
                String url = BLOCKCHAIN_API_BASE + "/block/" + blockId;
                String response = makeHttpRequest(url);
                
                if (response != null) {
                    JSONObject json = new JSONObject(response);
                    
                    BlockInfo blockInfo = new BlockInfo();
                    blockInfo.hash = json.getString("id");
                    blockInfo.height = json.getInt("height");
                    blockInfo.timestamp = json.getLong("timestamp");
                    blockInfo.transactionCount = json.getInt("tx_count");
                    blockInfo.size = json.getInt("size");
                    blockInfo.weight = json.getInt("weight");
                    blockInfo.difficulty = json.getDouble("difficulty");
                    blockInfo.nonce = json.getLong("nonce");
                    blockInfo.previousBlockHash = json.optString("previousblockhash", "");
                    blockInfo.merkleRoot = json.optString("merkle_root", "");
                    
                    return blockInfo;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error looking up block", e);
                error = "Failed to lookup block: " + e.getMessage();
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(BlockInfo result) {
            if (result != null) {
                listener.onBlockInfoReceived(result);
            } else {
                listener.onError(error != null ? error : "Failed to lookup block");
            }
        }
    }
    
    /**
     * Network statistics task
     */
    private class NetworkStatsTask extends AsyncTask<Void, Void, NetworkStats> {
        private BlockchainExplorerListener listener;
        private String error;
        
        public NetworkStatsTask(BlockchainExplorerListener listener) {
            this.listener = listener;
        }
        
        @Override
        protected NetworkStats doInBackground(Void... voids) {
            try {
                // Get latest block for network stats
                String response = makeHttpRequest(BLOCKCHAIN_API_BASE + "/blocks/tip/height");
                
                if (response != null) {
                    int currentHeight = Integer.parseInt(response.trim());
                    
                    // Get block info for difficulty and other stats
                    String blockResponse = makeHttpRequest(BLOCKCHAIN_API_BASE + "/block-height/" + currentHeight);
                    if (blockResponse != null) {
                        JSONObject blockJson = new JSONObject(blockResponse);
                        
                        NetworkStats stats = new NetworkStats();
                        stats.currentBlockHeight = currentHeight;
                        stats.difficulty = blockJson.getDouble("difficulty");
                        stats.hashRate = calculateHashRate(stats.difficulty);
                        stats.lastBlockTime = blockJson.getLong("timestamp");
                        
                        return stats;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting network stats", e);
                error = "Failed to get network stats: " + e.getMessage();
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(NetworkStats result) {
            if (result != null) {
                // Create a BlockInfo object to reuse the existing interface
                BlockInfo blockInfo = new BlockInfo();
                blockInfo.height = result.currentBlockHeight;
                blockInfo.difficulty = result.difficulty;
                blockInfo.timestamp = result.lastBlockTime;
                listener.onBlockInfoReceived(blockInfo);
            } else {
                listener.onError(error != null ? error : "Failed to get network stats");
            }
        }
    }
    
    /**
     * Make HTTP request to API endpoint
     */
    private String makeHttpRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(15000); // 15 seconds
            connection.setRequestProperty("User-Agent", "BitcoinMiner/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                Log.w(TAG, "HTTP request failed with code: " + responseCode);
            }
            
        } catch (IOException e) {
            Log.e(TAG, "HTTP request failed", e);
        }
        return null;
    }
    
    /**
     * Get current block height
     */
    private int getCurrentBlockHeight() {
        try {
            String response = makeHttpRequest(BLOCKCHAIN_API_BASE + "/blocks/tip/height");
            if (response != null) {
                return Integer.parseInt(response.trim());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get current block height", e);
        }
        return 0;
    }
    
    /**
     * Calculate network hash rate from difficulty
     */
    private double calculateHashRate(double difficulty) {
        // Simplified hash rate calculation (TH/s)
        return difficulty * Math.pow(2, 32) / (10 * 60 * Math.pow(10, 12));
    }
    
    /**
     * Format satoshis to BTC
     */
    public String formatBTC(long satoshis) {
        return btcFormat.format(satoshis / 100000000.0);
    }
    
    /**
     * Format price
     */
    public String formatPrice(double price) {
        return priceFormat.format(price);
    }
    
    /**
     * Format timestamp
     */
    public String formatTimestamp(long timestamp) {
        return dateFormat.format(new Date(timestamp * 1000));
    }
    
    // Data classes
    public static class AddressInfo {
        public String address;
        public long balance; // in satoshis
        public long totalReceived; // in satoshis
        public long totalSent; // in satoshis
        public int transactionCount;
        public List<TransactionSummary> recentTransactions;
    }
    
    public static class TransactionInfo {
        public String txid;
        public String blockHash;
        public int blockHeight;
        public int confirmations;
        public long timestamp;
        public int size;
        public long fee; // in satoshis
        public long inputValue; // in satoshis
        public long outputValue; // in satoshis
        public boolean confirmed;
    }
    
    public static class TransactionSummary {
        public String txid;
        public int confirmations;
        public long timestamp;
    }
    
    public static class PriceInfo {
        public double priceUSD;
        public String currency;
        public String lastUpdated;
    }
    
    public static class BlockInfo {
        public String hash;
        public int height;
        public long timestamp;
        public int transactionCount;
        public int size;
        public int weight;
        public double difficulty;
        public long nonce;
        public String previousBlockHash;
        public String merkleRoot;
    }
    
    public static class NetworkStats {
        public int currentBlockHeight;
        public double difficulty;
        public double hashRate; // TH/s
        public long lastBlockTime;
    }
}

