package com.nduyuwilson.thitima.data.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.gson.Gson;
import com.nduyuwilson.thitima.data.AppDatabase;
import com.nduyuwilson.thitima.data.model.BackupData;
import com.nduyuwilson.thitima.data.model.SettingsData;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRepository {
    private AppDatabase db;
    private Application application;

    public BackupRepository(Application application) {
        this.application = application;
        this.db = AppDatabase.getDatabase(application);
    }

    public Future<File> createFullBackupZip() {
        return AppDatabase.databaseWriteExecutor.submit(() -> {
            File backupDir = application.getExternalCacheDir();
            File zipFile = new File(backupDir, "Thitima_Full_Backup.zip");
            
            // 1. Generate JSON with Settings
            BackupData data = new BackupData();
            data.projects = db.projectDao().getAllProjectsSync();
            data.items = db.itemDao().getAllItemsSync();
            data.itemVariants = db.itemVariantDao().getAllVariantsSync();
            data.projectItems = db.projectItemDao().getAllProjectItemsSync();
            
            SharedPreferences prefs = application.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
            SettingsData settings = new SettingsData();
            settings.businessName = prefs.getString("business_name", "THITIMA ELECTRICALS");
            settings.userName = prefs.getString("user_name", "");
            settings.userNumber = prefs.getString("user_number", "");
            settings.bankDetails = prefs.getString("bank_details", "");
            settings.paybillNumber = prefs.getString("paybill_number", "");
            settings.paybillAccount = prefs.getString("paybill_account", "");
            settings.tillNumber = prefs.getString("till_number", "");
            settings.themeMode = prefs.getInt("theme_mode", 2);
            settings.currencySymbol = prefs.getString("currency_symbol", "Ksh");
            data.settings = settings;

            String json = new Gson().toJson(data);

            // 2. Zip everything
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
                ZipEntry jsonEntry = new ZipEntry("backup.json");
                zos.putNextEntry(jsonEntry);
                zos.write(json.getBytes());
                zos.closeEntry();

                File filesDir = application.getFilesDir();
                File[] files = filesDir.listFiles();
                if (files != null) {
                    byte[] buffer = new byte[1024];
                    for (File file : files) {
                        if (file.isFile() && file.getName().startsWith("IMG_")) {
                            ZipEntry imgEntry = new ZipEntry("images/" + file.getName());
                            zos.putNextEntry(imgEntry);
                            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                                int bytesRead;
                                while ((bytesRead = bis.read(buffer)) != -1) {
                                    zos.write(buffer, 0, bytesRead);
                                }
                            }
                            zos.closeEntry();
                        }
                    }
                }
            }
            return zipFile;
        });
    }

    public void restoreFromZip(Context context, Uri uri, RestoreCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
                
                ZipEntry entry;
                String json = null;
                File filesDir = application.getFilesDir();

                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("backup.json")) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                        StringBuilder sb = new StringBuilder();
                        char[] charBuffer = new char[1024];
                        int readCount;
                        while ((readCount = reader.read(charBuffer)) != -1) {
                            sb.append(charBuffer, 0, readCount);
                        }
                        json = sb.toString();
                    } else if (entry.getName().startsWith("images/")) {
                        String fileName = entry.getName().substring(7);
                        File destFile = new File(filesDir, fileName);
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile))) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zis.read(buffer)) != -1) {
                                bos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }

                if (json != null) {
                    BackupData data = new Gson().fromJson(json, BackupData.class);
                    db.runInTransaction(() -> {
                        db.projectItemDao().deleteAll();
                        db.itemVariantDao().deleteAll();
                        db.projectDao().deleteAll();
                        db.itemDao().deleteAll();

                        if (data.items != null) {
                            for (var item : data.items) db.itemDao().insert(item);
                        }
                        if (data.projects != null) {
                            for (var project : data.projects) db.projectDao().insert(project);
                        }
                        if (data.itemVariants != null) {
                            for (var variant : data.itemVariants) db.itemVariantDao().insert(variant);
                        }
                        if (data.projectItems != null) {
                            for (var pItem : data.projectItems) db.projectItemDao().insert(pItem);
                        }
                        
                        // Restore Settings
                        if (data.settings != null) {
                            SharedPreferences.Editor editor = application.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE).edit();
                            editor.putString("business_name", data.settings.businessName);
                            editor.putString("user_name", data.settings.userName);
                            editor.putString("user_number", data.settings.userNumber);
                            editor.putString("bank_details", data.settings.bankDetails);
                            editor.putString("paybill_number", data.settings.paybillNumber);
                            editor.putString("paybill_account", data.settings.paybillAccount);
                            editor.putString("till_number", data.settings.tillNumber);
                            editor.putInt("theme_mode", data.settings.themeMode);
                            editor.putString("currency_symbol", data.settings.currencySymbol);
                            editor.apply();
                        }
                    });
                    callback.onSuccess();
                } else {
                    callback.onError("No backup data found in zip");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public interface RestoreCallback {
        void onSuccess();
        void onError(String message);
    }
}
