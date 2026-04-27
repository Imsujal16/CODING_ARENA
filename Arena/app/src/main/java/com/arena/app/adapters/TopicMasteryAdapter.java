package com.arena.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.models.TopicMastery;

import java.util.ArrayList;
import java.util.List;

public class TopicMasteryAdapter extends RecyclerView.Adapter<TopicMasteryAdapter.ViewHolder> {

    private List<TopicMastery> items = new ArrayList<>();

    public void setItems(List<TopicMastery> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_topic_mastery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName, textNext, textProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_topic_name);
            textNext = itemView.findViewById(R.id.text_next_problem);
            textProgress = itemView.findViewById(R.id.text_topic_progress);
        }

        void bind(TopicMastery item) {
            textName.setText(item.getName());
            textNext.setText("Next: " + item.getNextProblem());
            textProgress.setText(item.getProgressText());
        }
    }
}
