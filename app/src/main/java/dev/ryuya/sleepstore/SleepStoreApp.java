package dev.ryuya.sleepstore;
import android.app.Application;
import com.google.android.material.color.DynamicColors;
public final class SleepStoreApp extends Application {
    @Override public void onCreate() { super.onCreate(); DynamicColors.applyToActivitiesIfAvailable(this); }
}
