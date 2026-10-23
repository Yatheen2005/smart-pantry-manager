package za.ac.richfield.smartpantry.model;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Domain model representing a single ingredient stored in the user's pantry.
 */
public class PantryItem implements Serializable {

    private long id;
    private String name;
    private String normalizedName;
    private double quantity;
    private String unit;
    private String expiryDate; // Optional ISO format YYYY-MM-DD (may be null or empty)
    private long updatedAt;

    public PantryItem() {
        this.updatedAt = System.currentTimeMillis();
    }

    public PantryItem(long id, String name, String normalizedName,
                      double quantity, String unit, String expiryDate, long updatedAt) {
        this.id = id;
        this.name = name;
        this.normalizedName = normalizedName;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.updatedAt = updatedAt;
    }

    public PantryItem(String name, String normalizedName,
                      double quantity, String unit, String expiryDate) {
        this(0, name, normalizedName, quantity, unit, expiryDate, System.currentTimeMillis());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean hasExpiryDate() {
        return expiryDate != null && !expiryDate.trim().isEmpty();
    }

    /**
     * Returns the number of whole days from today (YYYY-MM-DD) until the item's expiry date.
     * Returns Long.MAX_VALUE if no expiry date is set or if parsing fails.
     */
    public long getDaysUntilExpiry(String todayIsoDate) {
        if (!hasExpiryDate()) {
            return Long.MAX_VALUE;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setLenient(false);
        try {
            Date today = sdf.parse(todayIsoDate);
            Date expiry = sdf.parse(expiryDate.trim());
            if (today == null || expiry == null) {
                return Long.MAX_VALUE;
            }
            // Round rather than truncate: a day can be 23 or 25 hours across daylight-saving changes
            long diffMillis = expiry.getTime() - today.getTime();
            return Math.round(diffMillis / (double) TimeUnit.DAYS.toMillis(1));
        } catch (ParseException e) {
            return Long.MAX_VALUE;
        }
    }

    public long getDaysUntilExpiry() {
        return getDaysUntilExpiry(todayIsoDate());
    }

    /** An item is expired from the day AFTER its expiry date (it is still usable on the date). */
    public boolean isExpired() {
        return getDaysUntilExpiry() < 0;
    }

    /** Today's date in the same YYYY-MM-DD format used for expiry dates. */
    public static String todayIsoDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public boolean isExpiringSoon(int warningDaysThreshold) {
        long days = getDaysUntilExpiry();
        return days >= 0 && days <= warningDaysThreshold;
    }

    /**
     * Formats quantities cleanly without trailing ".0" for whole numbers.
     */
    public String getFormattedQuantity() {
        return formatQuantityValue(quantity) + " " + unit;
    }

    public static String formatQuantityValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return String.format(Locale.US, "%d", Math.round(value));
        }
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
