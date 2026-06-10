package com.arena.app.adapters;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.models.LeaderboardEntry;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    public interface OnLeaderboardEntryClickListener {
        void onLeaderboardEntryClicked(LeaderboardEntry entry);
    }

    private List<LeaderboardEntry> items = new ArrayList<>();
    private OnLeaderboardEntryClickListener listener;

    public void setItems(List<LeaderboardEntry> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    public void setListener(OnLeaderboardEntryClickListener listener) {
        this.listener = listener;
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
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ConstraintLayout root;
        private final TextView textRank, textUsername, textSubtitle, textXp;
        private final TextView textAvatarInitial;
        private final CircleImageView imageAvatar;
        private final android.widget.LinearLayout containerScore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.leaderboard_item_root);
            textRank = itemView.findViewById(R.id.text_lb_rank);
            textUsername = itemView.findViewById(R.id.text_lb_username);
            textSubtitle = itemView.findViewById(R.id.text_lb_subtitle);
            textXp = itemView.findViewById(R.id.text_lb_xp);
            textAvatarInitial = itemView.findViewById(R.id.text_lb_avatar_initial);
            imageAvatar = itemView.findViewById(R.id.img_lb_avatar);
            containerScore = itemView.findViewById(R.id.container_score);
        }

        void bind(LeaderboardEntry entry, OnLeaderboardEntryClickListener listener) {
            textRank.setText(String.valueOf(entry.getRank()));
            textUsername.setText(entry.getUsername());
            textSubtitle.setText(entry.getXp() + " - PUZZLE");
            textXp.setText(entry.getFormattedXp());
            bindAvatar(entry);
            root.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLeaderboardEntryClicked(entry);
                }
            });

            // Badge color logic for top 3
            if (entry.getRank() == 1) {
                textRank.setBackgroundColor(itemView.getContext().getColor(R.color.champion_gold));
                textRank.setTextColor(itemView.getContext().getColor(android.R.color.black));
                // Rank 1: score pill gets a neon purple background
                containerScore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#3A2060")));
            } else if (entry.getRank() == 2) {
                textRank.setBackgroundColor(itemView.getContext().getColor(R.color.silver));
                textRank.setTextColor(itemView.getContext().getColor(android.R.color.black));
                containerScore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.TRANSPARENT));
            } else if (entry.getRank() == 3) {
                textRank.setBackgroundColor(itemView.getContext().getColor(R.color.bronze));
                textRank.setTextColor(itemView.getContext().getColor(android.R.color.black));
                containerScore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.TRANSPARENT));
            } else {
                textRank.setBackgroundResource(R.drawable.bg_chip_unselected);
                textRank.setTextColor(itemView.getContext().getColor(android.R.color.white));
                containerScore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.TRANSPARENT));
            }

            if (entry.isCurrentUser()) {
                root.setBackgroundColor(itemView.getContext().getColor(R.color.leaderboard_highlight));
            } else {
                root.setBackgroundColor(itemView.getContext().getColor(R.color.background));
            }
        }

        private void bindAvatar(LeaderboardEntry entry) {
            String username = entry.getUsername() == null || entry.getUsername().trim().isEmpty()
                    ? "A"
                    : entry.getUsername().trim();
            textAvatarInitial.setText(String.valueOf(username.charAt(0)).toUpperCase());

            if (entry.getAvatarUrl() != null && !entry.getAvatarUrl().trim().isEmpty()) {
                textAvatarInitial.setVisibility(View.GONE);
                Glide.with(itemView)
                        .load(Uri.parse(entry.getAvatarUrl()))
                        .centerCrop()
                        .placeholder(R.drawable.bg_avatar_circle)
                        .error(R.drawable.bg_avatar_circle)
                        .into(imageAvatar);
                return;
            }

            Glide.with(itemView).clear(imageAvatar);
            imageAvatar.setImageDrawable(null);
            imageAvatar.setCircleBackgroundColor(resolveAvatarColor(entry));
            textAvatarInitial.setVisibility(View.VISIBLE);
        }

        private int resolveAvatarColor(LeaderboardEntry entry) {
            int[] colors = {
                    Color.parseColor("#1D75D8"),
                    Color.parseColor("#16C7F3"),
                    Color.parseColor("#8B7CFF"),
                    Color.parseColor("#3FD6C1")
            };
            String key = entry.getUserId() != null ? entry.getUserId() : entry.getUsername();
            int index = key == null ? 0 : Math.abs(key.hashCode()) % colors.length;
            return colors[index];
        }
    }
}
