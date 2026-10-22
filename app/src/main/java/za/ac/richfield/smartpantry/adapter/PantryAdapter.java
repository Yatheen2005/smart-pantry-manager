package za.ac.richfield.smartpantry.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.model.PantryItem;

/**
 * Custom RecyclerView Adapter binding SQLite PantryItem records to item_pantry.xml cards.
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
        holder.tvIngredientName.setText(item.getName());
        holder.tvQuantityUnit.setText("Available: " + item.getFormattedQuantity());

        if (item.hasExpiryDate()) {
            holder.tvExpiryBadge.setVisibility(View.VISIBLE);
            long days = item.getDaysUntilExpiry();
            if (days < 0) {
                holder.tvExpiryBadge.setText("EXPIRED (" + item.getExpiryDate() + ")");
                holder.tvExpiryBadge.setBackgroundResource(R.drawable.bg_badge_red);
            } else if (showExpiryAlerts && days <= expiryWarningDays) {
                holder.tvExpiryBadge.setText("Expiring in " + days + "d (" + item.getExpiryDate() + ")");
                holder.tvExpiryBadge.setBackgroundResource(R.drawable.bg_badge_amber);
            } else {
                holder.tvExpiryBadge.setText("Expires: " + item.getExpiryDate());
                holder.tvExpiryBadge.setBackgroundResource(R.drawable.bg_badge_green);
            }
        } else {
            holder.tvExpiryBadge.setVisibility(View.GONE);
        }

        holder.btnEditItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditItem(item);
            }
        });

        holder.btnDeleteItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteItem(item);
            }
        });
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
