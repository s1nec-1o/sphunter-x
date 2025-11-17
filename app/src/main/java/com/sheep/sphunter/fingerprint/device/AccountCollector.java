package com.sheep.sphunter.fingerprint.device;

import android.accounts.AccountManager;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * 账户信息采集器
 */
public class AccountCollector {
    private final Context context;

    public AccountCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 获取设备上的账户信息
     *
     * @return 账户信息字符串，包含账户类型和账户名称
     */
    @NonNull
    public String getAccountInfo() {
        try {
            if (context == null) {
                return "Context is null";
            }

            AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
            if (accountManager == null) {
                return "AccountManager is null";
            }

            android.accounts.Account[] accounts = accountManager.getAccounts();
            if (accounts.length == 0) {
                return "No accounts found";
            }

            StringBuilder result = new StringBuilder();
            result.append("Total accounts: ").append(accounts.length).append("\n\n");

            for (int i = 0; i < accounts.length; i++) {
                android.accounts.Account account = accounts[i];
                result.append("Account ").append(i + 1).append(":\n");
                result.append("  Type: ").append(account.type).append("\n");
                result.append("  Name: ").append(account.name).append("\n");
                if (i < accounts.length - 1) {
                    result.append("\n");
                }
            }

            return result.toString();
        } catch (SecurityException e) {
            return "SecurityException: " + e.getMessage() + "\n" +
                   "(Note: GET_ACCOUNTS permission is required on Android 6.0+)";
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }
}

