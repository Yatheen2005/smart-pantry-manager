package za.ac.richfield.smartpantry.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages persistent application settings via Android SharedPreferences for the Settings screen.
 */
public class UserPreferencesManager {

    private static final String PREFS_NAME = "smart_pantry_preferences";
    private static final String KEY_EXPIRY_ALERTS_ENABLED = "expiry_alerts_enabled";
    private static final String KEY_EXPIRY_THRESHOLD_DAYS = "expiry_threshold_days";
    private static final String KEY_EXCLUDE_EXPIRED_MATCHING = "exclude_expired_matching";
    private static final String KEY_SHOW_ALMOST_THERE = "show_almost_there";
    private static final String KEY_PREFERRED_UNIT_SYSTEM = "preferred_unit_system";

    public static final String UNIT_SYSTEM_METRIC = "METRIC";
    public static final String UNIT_SYSTEM_IMPERIAL = "IMPERIAL";

    private final SharedPreferences prefs;

    public UserPreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isExpiryAlertsEnabled() {
        return prefs.getBoolean(KEY_EXPIRY_ALERTS_ENABLED, true);
    }

    public void setExpiryAlertsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_EXPIRY_ALERTS_ENABLED, enabled).apply();
    }

    public int getExpiryThresholdDays() {
        return prefs.getInt(KEY_EXPIRY_THRESHOLD_DAYS, 3);
    }

    public void setExpiryThresholdDays(int days) {
        prefs.edit().putInt(KEY_EXPIRY_THRESHOLD_DAYS, Math.max(1, Math.min(30, days))).apply();
    }

    public boolean isExcludeExpiredFromMatching() {
        return prefs.getBoolean(KEY_EXCLUDE_EXPIRED_MATCHING, true);
    }

    public void setExcludeExpiredFromMatching(boolean exclude) {
        prefs.edit().putBoolean(KEY_EXCLUDE_EXPIRED_MATCHING, exclude).apply();
    }

    public boolean isShowAlmostThereSection() {
        return prefs.getBoolean(KEY_SHOW_ALMOST_THERE, true);
    }

    public void setShowAlmostThereSection(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_ALMOST_THERE, show).apply();
    }

    public String getPreferredUnitSystem() {
        return prefs.getString(KEY_PREFERRED_UNIT_SYSTEM, UNIT_SYSTEM_METRIC);
    }

    public void setPreferredUnitSystem(String system) {
        prefs.edit().putString(KEY_PREFERRED_UNIT_SYSTEM, system).apply();
    }
}
