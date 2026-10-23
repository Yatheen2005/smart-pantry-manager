package za.ac.richfield.smartpantry.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.model.IngredientCheck;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeMatchResult;

/**
 * Custom RecyclerView Adapter that binds RecipeMatchResult objects to item_recipe.xml cards.
 * The same adapter is used for the strict suggestions list and the separate "Almost There"
 * list; each card's badge is chosen from the result itself, so an almost-there recipe can
 * never be shown as READY.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(RecipeMatchResult matchResult);
    }

    private List<RecipeMatchResult> results = new ArrayList<>();
    private final OnRecipeClickListener listener;

    public RecipeAdapter(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<RecipeMatchResult> newResults) {
        this.results = (newResults != null) ? newResults : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        final RecipeMatchResult result = results.get(position);
        final Recipe recipe = result.getRecipe();
        Context context = holder.itemView.getContext();

        holder.tvRecipeTitle.setText(recipe.getTitle());
        holder.tvRecipeMeta.setText(context.getString(R.string.recipe_meta,
                recipe.getCategory(), recipe.getPrepTimeMinutes(), recipe.getServings(),
                result.getTotalIngredientsCount()));

        if (result.isStrictMatch()) {
            holder.tvMatchBadge.setText(R.string.badge_ready);
            holder.tvMatchBadge.setBackgroundResource(R.drawable.bg_badge_green);
            holder.tvMatchBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_green_dark));
            holder.tvMatchSummary.setText(context.getString(
                    R.string.recipe_all_available, result.getTotalIngredientsCount()));
            holder.tvMatchSummary.setTextColor(ContextCompat.getColor(context, R.color.primary_green));
        } else {
            holder.tvMatchBadge.setText(R.string.badge_missing_one);
            holder.tvMatchBadge.setBackgroundResource(R.drawable.bg_badge_amber);
            holder.tvMatchBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_amber_text));
            List<IngredientCheck> unsatisfied = result.getUnsatisfiedChecks();
            String firstProblem = unsatisfied.isEmpty() ? "" : unsatisfied.get(0).describe();
            holder.tvMatchSummary.setText(context.getString(R.string.recipe_still_needed, firstProblem));
            holder.tvMatchSummary.setTextColor(ContextCompat.getColor(context, R.color.accent_amber_text));
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(result));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final TextView tvRecipeTitle;
        final TextView tvMatchBadge;
        final TextView tvRecipeMeta;
        final TextView tvMatchSummary;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecipeTitle = itemView.findViewById(R.id.tvRecipeTitle);
            tvMatchBadge = itemView.findViewById(R.id.tvMatchBadge);
            tvRecipeMeta = itemView.findViewById(R.id.tvRecipeMeta);
            tvMatchSummary = itemView.findViewById(R.id.tvMatchSummary);
        }
    }
}
