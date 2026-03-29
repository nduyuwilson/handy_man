package com.nduyuwilson.thitima.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.nduyuwilson.thitima.data.dao.ItemDao;
import com.nduyuwilson.thitima.data.dao.ItemVariantDao;
import com.nduyuwilson.thitima.data.dao.LabourActivityDao;
import com.nduyuwilson.thitima.data.dao.PaymentDao;
import com.nduyuwilson.thitima.data.dao.ProjectDao;
import com.nduyuwilson.thitima.data.dao.ProjectItemDao;
import com.nduyuwilson.thitima.data.dao.RulesTemplateDao;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.data.entity.Payment;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.data.entity.RulesTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Item.class, Project.class, ProjectItem.class, ItemVariant.class, LabourActivity.class, RulesTemplate.class, Payment.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ItemDao itemDao();
    public abstract ProjectDao projectDao();
    public abstract ProjectItemDao projectItemDao();
    public abstract ItemVariantDao itemVariantDao();
    public abstract LabourActivityDao labourActivityDao();
    public abstract RulesTemplateDao rulesTemplateDao();
    public abstract PaymentDao paymentDao();

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
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
