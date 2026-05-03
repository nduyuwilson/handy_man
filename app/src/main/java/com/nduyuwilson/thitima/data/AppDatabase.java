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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Item.class, Category.class, Project.class, ProjectItem.class, ItemVariant.class, LabourActivity.class, RulesTemplate.class, Payment.class, Worker.class, WorkerPayment.class}, version = 7, exportSchema = false)
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

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "thitima_database")
                            .addMigrations(MIGRATION_6_7)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
