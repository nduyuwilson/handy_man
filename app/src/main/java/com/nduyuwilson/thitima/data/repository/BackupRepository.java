package com.nduyuwilson.thitima.data.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nduyuwilson.thitima.data.AppDatabase;
import com.nduyuwilson.thitima.data.entity.Category;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.data.entity.Payment;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.data.entity.RulesTemplate;
import com.nduyuwilson.thitima.data.entity.Worker;
import com.nduyuwilson.thitima.data.entity.WorkerPayment;
import com.nduyuwilson.thitima.data.model.BackupData;
import com.nduyuwilson.thitima.data.model.PaymentMethod;
import com.nduyuwilson.thitima.data.model.SettingsData;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRepository {
    private Application application;

    public BackupRepository(Application application) {
        this.application = application;
    }

    public Future<File> createFullBackupZip() {
        return AppDatabase.databaseWriteExecutor.submit(() -> {
            AppDatabase currentDb = AppDatabase.getDatabase(application);
            File backupDir = application.getExternalCacheDir();
            File zipFile = new File(backupDir, "Thitima_Full_Backup.zip");
            
            // 1. Generate JSON with Settings and all Database Tables
            BackupData data = new BackupData();
            data.projects = currentDb.projectDao().getAllProjectsSync();
            data.items = currentDb.itemDao().getAllItemsSync();
            data.itemVariants = currentDb.itemVariantDao().getAllVariantsSync();
            data.categories = currentDb.categoryDao().getAllCategoriesSync();
            data.projectItems = currentDb.projectItemDao().getAllProjectItemsSync();
            data.labourActivities = currentDb.labourActivityDao().getAllActivitiesSync();
            data.payments = currentDb.paymentDao().getAllPaymentsSync();
            data.rulesTemplates = currentDb.rulesTemplateDao().getAllTemplatesSync();
            data.workers = currentDb.workerDao().getAllWorkersSync();
            data.workerPayments = currentDb.workerPaymentDao().getAllWorkerPaymentsSync();
            
            SharedPreferences prefs = application.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
            SettingsData settings = new SettingsData();
            settings.businessName = prefs.getString("business_name", "THITIMA ELECTRICALS");
            settings.userName = prefs.getString("user_name", "");
            settings.userNumber = prefs.getString("user_number", "");
            settings.themeMode = prefs.getInt("theme_mode", 2);
            settings.currencySymbol = prefs.getString("currency_symbol", "Ksh");
            
            // Backup payment methods list
            String paymentJson = prefs.getString("payment_methods_json", "[]");
            Type listType = new TypeToken<ArrayList<PaymentMethod>>(){}.getType();
            settings.paymentMethods = new Gson().fromJson(paymentJson, listType);

            // Complete backup of all SharedPreferences entries
            Map<String, ?> allEntries = prefs.getAll();
            if (allEntries != null) {
                settings.allPreferences = new HashMap<>(allEntries);
            }
            
            data.settings = settings;

            String json = new Gson().toJson(data);

            // 2. Zip backup.json and all stored media images
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
                        if (file.isFile() && (file.getName().startsWith("IMG_") 
                                || file.getName().toLowerCase().endsWith(".jpg") 
                                || file.getName().toLowerCase().endsWith(".png") 
                                || file.getName().toLowerCase().endsWith(".jpeg"))) {
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
            AppDatabase currentDb = AppDatabase.getDatabase(application);
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
                        if (!fileName.isEmpty()) {
                            File destFile = new File(filesDir, fileName);
                            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile))) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = zis.read(buffer)) != -1) {
                                    bos.write(buffer, 0, len);
                                }
                            }
                        }
                    }
                    zis.closeEntry();
                }

                if (json != null) {
                    BackupData data = new Gson().fromJson(json, BackupData.class);
                    currentDb.runInTransaction(() -> {
                        // 1. Delete child tables first to respect foreign key cascades
                        currentDb.workerPaymentDao().deleteAll();
                        currentDb.paymentDao().deleteAll();
                        currentDb.projectItemDao().deleteAll();
                        currentDb.labourActivityDao().deleteAll();
                        currentDb.itemVariantDao().deleteAll();
                        currentDb.workerDao().deleteAll();
                        currentDb.projectDao().deleteAll();
                        currentDb.itemDao().deleteAll();
                        currentDb.categoryDao().deleteAll();
                        currentDb.rulesTemplateDao().deleteAll();

                        // 2. Insert parent tables first to satisfy foreign key constraints
                        if (data.categories != null) {
                            for (Category category : data.categories) currentDb.categoryDao().insert(category);
                        }
                        if (data.items != null) {
                            for (Item item : data.items) currentDb.itemDao().insert(item);
                        }
                        if (data.itemVariants != null) {
                            for (ItemVariant variant : data.itemVariants) currentDb.itemVariantDao().insert(variant);
                        }
                        if (data.projects != null) {
                            for (Project project : data.projects) currentDb.projectDao().insert(project);
                        }
                        if (data.workers != null) {
                            for (Worker worker : data.workers) currentDb.workerDao().insert(worker);
                        }
                        if (data.rulesTemplates != null) {
                            for (RulesTemplate template : data.rulesTemplates) currentDb.rulesTemplateDao().insert(template);
                        }
                        if (data.projectItems != null) {
                            for (ProjectItem pItem : data.projectItems) currentDb.projectItemDao().insert(pItem);
                        }
                        if (data.labourActivities != null) {
                            for (LabourActivity activity : data.labourActivities) currentDb.labourActivityDao().insert(activity);
                        }
                        if (data.payments != null) {
                            for (Payment payment : data.payments) currentDb.paymentDao().insert(payment);
                        }
                        if (data.workerPayments != null) {
                            for (WorkerPayment wPayment : data.workerPayments) currentDb.workerPaymentDao().insert(wPayment);
                        }
                        
                        // 3. Restore all app settings and preferences
                        if (data.settings != null) {
                            SharedPreferences.Editor editor = application.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE).edit();
                            
                            // Restore dynamic map entries if present
                            if (data.settings.allPreferences != null) {
                                for (Map.Entry<String, Object> prefEntry : data.settings.allPreferences.entrySet()) {
                                    String key = prefEntry.getKey();
                                    Object val = prefEntry.getValue();
                                    if (val instanceof String) {
                                        editor.putString(key, (String) val);
                                    } else if (val instanceof Boolean) {
                                        editor.putBoolean(key, (Boolean) val);
                                    } else if (val instanceof Integer) {
                                        editor.putInt(key, (Integer) val);
                                    } else if (val instanceof Long) {
                                        editor.putLong(key, (Long) val);
                                    } else if (val instanceof Float) {
                                        editor.putFloat(key, (Float) val);
                                    } else if (val instanceof Double) {
                                        double d = (Double) val;
                                        if (d == Math.floor(d) && !Double.isInfinite(d)) {
                                            editor.putInt(key, (int) d);
                                        } else {
                                            editor.putFloat(key, (float) d);
                                        }
                                    }
                                }
                            }

                            // Explicit field restoration for guaranteed compatibility
                            if (data.settings.businessName != null) editor.putString("business_name", data.settings.businessName);
                            if (data.settings.userName != null) editor.putString("user_name", data.settings.userName);
                            if (data.settings.userNumber != null) editor.putString("user_number", data.settings.userNumber);
                            editor.putInt("theme_mode", data.settings.themeMode);
                            if (data.settings.currencySymbol != null) editor.putString("currency_symbol", data.settings.currencySymbol);
                            if (data.settings.paymentMethods != null) {
                                editor.putString("payment_methods_json", new Gson().toJson(data.settings.paymentMethods));
                            }
                            
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
