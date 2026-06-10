package com.arena.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.models.ContinueLearning;

import java.util.ArrayList;
import java.util.List;

public class ContinueLearningAdapter extends RecyclerView.Adapter<ContinueLearningAdapter.ViewHolder> {

    public interface OnContinueLearningClickListener {
        void onContinueLearningClicked(ContinueLearning item);
    }

    private List<ContinueLearning> items = new ArrayList<>();
    private OnContinueLearningClickListener listener;

    public void setItems(List<ContinueLearning> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setListener(OnContinueLearningClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_continue_learning, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContinueLearning item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView textNext;
        private final TextView textTag;
        private final TextView textProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textNext = itemView.findViewById(R.id.text_next);
            textTag = itemView.findViewById(R.id.text_tag);
            textProgress = itemView.findViewById(R.id.text_progress);
        }

        void bind(ContinueLearning item, OnContinueLearningClickListener listener) {
            textTitle.setText(item.getTitle());
            textNext.setText("Next: " + item.getNextProblem());
            textTag.setText(item.getTopicTag());
            textProgress.setText(item.getProgressPercent() + "%");
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContinueLearningClicked(item);
                }
            });
        }
    }
}
