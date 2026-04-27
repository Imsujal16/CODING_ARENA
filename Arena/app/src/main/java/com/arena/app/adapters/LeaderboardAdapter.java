package com.arena.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.models.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardEntry> items = new ArrayList<>();

    public void setItems(List<LeaderboardEntry> items) {
        // Only show rank 4+ (top 3 shown in podium)
        this.items = new ArrayList<>();
        for (LeaderboardEntry entry : items) {
            if (entry.getRank() > 3) {
                this.items.add(entry);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ConstraintLayout root;
        private final TextView textRank, textUsername, textXp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.leaderboard_item_root);
            textRank = itemView.findViewById(R.id.text_lb_rank);
            textUsername = itemView.findViewById(R.id.text_lb_username);
            textXp = itemView.findViewById(R.id.text_lb_xp);
        }

        void bind(LeaderboardEntry entry) {
            textRank.setText(String.format("%02d", entry.getRank()));
            textUsername.setText(entry.getUsername());
            textXp.setText(entry.getFormattedXp());

            if (entry.isCurrentUser()) {
                root.setBackgroundResource(R.drawable.bg_leaderboard_highlight);
            } else {
                root.setBackgroundColor(itemView.getContext().getColor(R.color.card_background));
            }
        }
    }
}
