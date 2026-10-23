package za.ac.richfield.smartpantry.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.model.PantryItem;

/**
 * Custom RecyclerView Adapter that binds PantryItem rows from the SQLite pantry_items table
 * to item_pantry.xml cards. Edit/Delete taps are passed back to the Activity via a listener,
 * so the adapter only displays data and never touches the database itself.
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    public interface OnPantryItemActionListener {
        void onEditItem(PantryItem item);

        void onDeleteItem(PantryItem item);
    }

    private List<PantryItem> items = new ArrayList<>();
    private final OnPantryItemActionListener listener;
    private int expiryWarningDays = 3;
    private boolean showExpiryAlerts = true;

    public PantryAdapter(OnPantryItemActionListener listener) {
        this.listener = listener;
    }

    /** Replaces the displayed list (called after every database change). */
    public void updateData(List<PantryItem> newItems, boolean showExpiryAlerts, int warningDays) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        this.showExpiryAlerts = showExpiryAlerts;
        this.expiryWarningDays = warningDays;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        final PantryItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvIngredientName.setText(item.getName());
        holder.tvQuantityUnit.setText(context.getString(R.string.available_quantity, item.getFormattedQuantity()));
        bindExpiryBadge(holder.tvExpiryBadge, item, context);

        // Screen readers announce "Edit Butter" rather than just "Edit"
        holder.btnEditItem.setContentDescription(context.getString(R.string.cd_edit_item, item.getName()));
        holder.btnDeleteItem.setContentDescription(context.getString(R.string.cd_delete_item, item.getName()));

        holder.btnEditItem.setOnClickListener(v -> listener.onEditItem(item));
        holder.itemView.setOnClickListener(v -> listener.onEditItem(item));
        holder.btnDeleteItem.setOnClickListener(v -> listener.onDeleteItem(item));
    }

    private void bindExpiryBadge(TextView badge, PantryItem item, Context context) {
        if (!item.hasExpiryDate()) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setVisibility(View.VISIBLE);
        long days = item.getDaysUntilExpiry();
        String date = item.getExpiryDate();

        if (days < 0) {
            setBadge(badge, context.getString(R.string.badge_expired, date),
                    R.drawable.bg_badge_red, R.color.danger_red);
        } else if (showExpiryAlerts && days == 0) {
            setBadge(badge, context.getString(R.string.badge_expires_today, date),
                    R.drawable.bg_badge_amber, R.color.accent_amber_text);
        } else if (showExpiryAlerts && days <= expiryWarningDays) {
            setBadge(badge, context.getResources().getQuantityString(
                            R.plurals.badge_expiring_soon, (int) days, (int) days, date),
                    R.drawable.bg_badge_amber, R.color.accent_amber_text);
        } else {
            setBadge(badge, context.getString(R.string.badge_expires_on, date),
                    R.drawable.bg_badge_green, R.color.primary_green_dark);
        }
    }

    private static void setBadge(TextView badge, String text, int backgroundRes, int textColorRes) {
        badge.setText(text);
        badge.setBackgroundResource(backgroundRes);
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), textColorRes));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PantryViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIngredientName;
        final TextView tvQuantityUnit;
        final TextView tvExpiryBadge;
        final Button btnEditItem;
        final Button btnDeleteItem;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
            tvQuantityUnit = itemView.findViewById(R.id.tvQuantityUnit);
            tvExpiryBadge = itemView.findViewById(R.id.tvExpiryBadge);
            btnEditItem = itemView.findViewById(R.id.btnEditItem);
            btnDeleteItem = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}
