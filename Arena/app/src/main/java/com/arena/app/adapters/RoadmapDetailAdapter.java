package com.arena.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;

import java.util.ArrayList;
import java.util.List;

public class RoadmapDetailAdapter extends RecyclerView.Adapter<RoadmapDetailAdapter.ViewHolder> {

    public static class StriverProblem {
        public String title;
        public String slug;
        public String url;
        public String platform;
        public String topic;
        public String step;
        public boolean isSolved;
        public boolean isFirstInTopic;
    }

    private List<StriverProblem> items = new ArrayList<>();
    private OnProblemInteractionListener listener;

    public void setItems(List<StriverProblem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setListener(OnProblemInteractionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_striver_problem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StriverProblem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTopicHeader;
        private final TextView textTitle;
        private final TextView textPlatform;
        private final CheckBox checkboxSolved;
        private final ImageButton btnOpenLink;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTopicHeader = itemView.findViewById(R.id.text_topic_header);
            textTitle = itemView.findViewById(R.id.text_problem_title);
            textPlatform = itemView.findViewById(R.id.text_problem_platform);
            checkboxSolved = itemView.findViewById(R.id.checkbox_solved);
            btnOpenLink = itemView.findViewById(R.id.btn_open_link);
        }

        void bind(StriverProblem item, OnProblemInteractionListener listener) {
            textTitle.setText(item.title);
            textPlatform.setText(item.platform);

            if (item.isFirstInTopic) {
                textTopicHeader.setVisibility(View.VISIBLE);
                textTopicHeader.setText(item.topic);
            } else {
                textTopicHeader.setVisibility(View.GONE);
            }

            checkboxSolved.setOnCheckedChangeListener(null); // Clear listener to prevent recycling bugs
            checkboxSolved.setChecked(item.isSolved);

            checkboxSolved.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.isSolved = isChecked;
                if (listener != null) {
                    listener.onProblemChecked(item, isChecked);
                }
            });

            btnOpenLink.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProblemClicked(item);
                }
            });
            
            // Allow clicking the whole row to open the link
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProblemClicked(item);
                }
            });
        }
    }

    public interface OnProblemInteractionListener {
        void onProblemChecked(StriverProblem problem, boolean isChecked);
        void onProblemClicked(StriverProblem problem);
    }
}
