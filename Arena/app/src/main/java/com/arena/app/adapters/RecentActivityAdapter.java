package com.arena.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.models.Problem;

import java.util.ArrayList;
import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private List<Problem> items = new ArrayList<>();

    public void setItems(List<Problem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle, chipDifficulty, textTimeAgo;
        private final ImageView iconCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_activity_title);
            chipDifficulty = itemView.findViewById(R.id.chip_activity_difficulty);
            textTimeAgo = itemView.findViewById(R.id.text_time_ago);
            iconCheck = itemView.findViewById(R.id.icon_solved_check);
        }

        void bind(Problem item) {
            textTitle.setText(item.getDisplayTitle());
            chipDifficulty.setText(item.getDifficulty().toUpperCase());
            textTimeAgo.setText(item.getSolvedTimeAgo());

            iconCheck.setVisibility(item.isSolved() ? View.VISIBLE : View.GONE);

            switch (item.getDifficulty().toLowerCase()) {
                case "easy":
                    chipDifficulty.setBackgroundResource(R.drawable.bg_chip_easy);
                    chipDifficulty.setTextColor(itemView.getContext().getColor(R.color.difficulty_easy));
                    break;
                case "medium":
                    chipDifficulty.setBackgroundResource(R.drawable.bg_chip_medium);
                    chipDifficulty.setTextColor(itemView.getContext().getColor(R.color.difficulty_medium));
                    break;
                case "hard":
                    chipDifficulty.setBackgroundResource(R.drawable.bg_chip_hard);
                    chipDifficulty.setTextColor(itemView.getContext().getColor(R.color.difficulty_hard));
                    break;
            }
        }
    }
}
