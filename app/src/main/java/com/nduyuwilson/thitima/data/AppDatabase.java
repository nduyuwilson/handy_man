package com.nduyuwilson.thitima.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.nduyuwilson.thitima.data.dao.CategoryDao;
import com.nduyuwilson.thitima.data.dao.ItemDao;
import com.nduyuwilson.thitima.data.dao.ItemVariantDao;
import com.nduyuwilson.thitima.data.dao.LabourActivityDao;
import com.nduyuwilson.thitima.data.dao.PaymentDao;
import com.nduyuwilson.thitima.data.dao.ProjectDao;
import com.nduyuwilson.thitima.data.dao.ProjectItemDao;
import com.nduyuwilson.thitima.data.dao.RulesTemplateDao;
import com.nduyuwilson.thitima.data.dao.WorkerDao;
import com.nduyuwilson.thitima.data.dao.WorkerPaymentDao;
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

import com.nduyuwilson.thitima.auth.AuthManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Item.class, Category.class, Project.class, ProjectItem.class, ItemVariant.class, LabourActivity.class, RulesTemplate.class, Payment.class, Worker.class, WorkerPayment.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ItemDao itemDao();
    public abstract ProjectDao projectDao();
    public abstract ProjectItemDao projectItemDao();
    public abstract ItemVariantDao itemVariantDao();
    public abstract LabourActivityDao labourActivityDao();
    public abstract RulesTemplateDao rulesTemplateDao();
    public abstract PaymentDao paymentDao();
    public abstract WorkerDao workerDao();
    public abstract WorkerPaymentDao workerPaymentDao();
    public abstract CategoryDao categoryDao();

    /**
     * Migration from version 6 to 7:
     * 1. Create the 'categories' table.
     * 2. Add 'categoryId' column to the 'items' table.
     */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT)");
            database.execSQL("ALTER TABLE `items` ADD COLUMN `categoryId` INTEGER NOT NULL DEFAULT -1");
        }
    };

    /**
     * Migration from version 7 to 8:
     * Add 'mpesaMessage' column to 'payments' table.
     */
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `payments` ADD COLUMN `mpesaMessage` TEXT");
        }
    };

    private static volatile AppDatabase INSTANCE;
    private static volatile String currentDbName = null;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        String uid = AuthManager.getUid(context);
        String targetDbName = (uid != null && !uid.trim().isEmpty()) ? "thitima_db_" + uid.trim() : "thitima_database";

        // If user logged in and has legacy data in 'thitima_database' but targetDbName doesn't exist yet, migrate it!
        if (uid != null && !uid.trim().isEmpty()) {
            try {
                java.io.File targetDbFile = context.getDatabasePath(targetDbName);
                java.io.File legacyDbFile = context.getDatabasePath("thitima_database");
                if (!targetDbFile.exists() && legacyDbFile.exists()) {
                    copyFile(legacyDbFile, targetDbFile);
                    java.io.File legacyWal = new java.io.File(legacyDbFile.getAbsolutePath() + "-wal");
                    java.io.File targetWal = new java.io.File(targetDbFile.getAbsolutePath() + "-wal");
                    if (legacyWal.exists()) copyFile(legacyWal, targetWal);
                    java.io.File legacyShm = new java.io.File(legacyDbFile.getAbsolutePath() + "-shm");
                    java.io.File targetShm = new java.io.File(targetDbFile.getAbsolutePath() + "-shm");
                    if (legacyShm.exists()) copyFile(legacyShm, targetShm);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (INSTANCE == null || !targetDbName.equals(currentDbName)) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null || !targetDbName.equals(currentDbName)) {
                    if (INSTANCE != null && INSTANCE.isOpen()) {
                        INSTANCE.close();
                    }
                    currentDbName = targetDbName;
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, targetDbName)
                            .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void copyFile(java.io.File src, java.io.File dst) throws java.io.IOException {
        if (dst.getParentFile() != null && !dst.getParentFile().exists()) {
            dst.getParentFile().mkdirs();
        }
        try (java.io.InputStream in = new java.io.FileInputStream(src);
             java.io.OutputStream out = new java.io.FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public static synchronized void resetDatabaseInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
        }
        INSTANCE = null;
        currentDbName = null;
    }
}
