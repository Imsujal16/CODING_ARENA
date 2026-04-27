package com.arena.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.models.Problem;

import java.util.ArrayList;
import java.util.List;

public class ProblemListAdapter extends RecyclerView.Adapter<ProblemListAdapter.ViewHolder> {

    private List<Problem> items = new ArrayList<>();
    private OnProblemClickListener onProblemClickListener;

    public void setItems(List<Problem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setOnProblemClickListener(OnProblemClickListener onProblemClickListener) {
        this.onProblemClickListener = onProblemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_problem_upnext, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), onProblemClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView chipDifficulty;
        private final TextView textTopic;
        private final TextView textXp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_problem_title);
            chipDifficulty = itemView.findViewById(R.id.chip_problem_difficulty);
            textTopic = itemView.findViewById(R.id.text_problem_topic_label);
            textXp = itemView.findViewById(R.id.text_problem_xp);
        }

        void bind(Problem item, OnProblemClickListener listener) {
            textTitle.setText(item.getTitle());
            chipDifficulty.setText(item.getDifficulty());
            textTopic.setText("- " + item.getTopic());
            textXp.setText("+" + item.getXpReward() + " XP");
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProblemClicked(item);
                }
            });

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
                default:
                    chipDifficulty.setBackgroundResource(R.drawable.bg_chip_medium);
                    chipDifficulty.setTextColor(itemView.getContext().getColor(R.color.difficulty_medium));
                    break;
            }
        }
    }

    public interface OnProblemClickListener {
        void onProblemClicked(Problem problem);
    }
}
