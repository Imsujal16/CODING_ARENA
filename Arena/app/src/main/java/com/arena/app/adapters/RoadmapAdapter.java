package com.arena.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.models.Roadmap;

import java.util.ArrayList;
import java.util.List;

public class RoadmapAdapter extends RecyclerView.Adapter<RoadmapAdapter.ViewHolder> {

    private List<Roadmap> items = new ArrayList<>();

    public void setItems(List<Roadmap> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_roadmap_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ConstraintLayout bg;
        private final TextView textBadge, textTitle, textSubtitle, textProgress;
        private final ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.roadmap_bg);
            textBadge = itemView.findViewById(R.id.text_badge);
            textTitle = itemView.findViewById(R.id.text_roadmap_title);
            textSubtitle = itemView.findViewById(R.id.text_roadmap_subtitle);
            textProgress = itemView.findViewById(R.id.text_progress_percent);
            progressBar = itemView.findViewById(R.id.progress_roadmap);
        }

        void bind(Roadmap item) {
            textTitle.setText(item.getTitle());
            textSubtitle.setText(item.getSubtitle());
            textProgress.setText(item.getProgress() + "%");
            progressBar.setProgress(item.getProgress());

            if (item.getBadge() != null && !item.getBadge().isEmpty()) {
                textBadge.setVisibility(View.VISIBLE);
                textBadge.setText(item.getBadge());
            } else {
                textBadge.setVisibility(View.GONE);
            }

            bg.setBackgroundResource(item.isBlueGradient() ?
                    R.drawable.bg_roadmap_card_blue : R.drawable.bg_roadmap_card_green);
        }
    }
}
