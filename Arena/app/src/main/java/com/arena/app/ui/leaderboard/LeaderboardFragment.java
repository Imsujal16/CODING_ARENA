package com.arena.app.ui.leaderboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.adapters.LeaderboardAdapter;
import com.arena.app.models.LeaderboardEntry;
import com.arena.app.repository.LeaderboardRepository;

import java.util.List;

public class LeaderboardFragment extends Fragment {

    private LeaderboardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView(view);
        loadData(view);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_leaderboard);
        adapter = new LeaderboardAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadData(View view) {
        LeaderboardRepository repository = new LeaderboardRepository(requireContext());
        repository.getLeaderboard(null, 50).observe(getViewLifecycleOwner(), entries -> {
            if (entries == null || entries.isEmpty()) {
                return;
            }

            bindPodium(view, entries);
            adapter.setItems(entries);

            LeaderboardEntry currentUser = repository.findCurrentUser(entries);
            ((TextView) view.findViewById(R.id.text_current_rank))
                    .setText(String.valueOf(currentUser.getRank()));
            ((TextView) view.findViewById(R.id.text_current_username))
                    .setText(currentUser.getUsername());
            ((TextView) view.findViewById(R.id.text_total_xp_info))
                    .setText(String.format("%,d", currentUser.getXp()));
            ((TextView) view.findViewById(R.id.text_next_rank_info))
                    .setText(resolveNextRankText(entries, currentUser));
        });
    }

    private void bindPodium(View view, List<LeaderboardEntry> entries) {
        bindPodiumEntry(view, entries, 0, R.id.text_rank1_name, R.id.text_rank1_xp);
        bindPodiumEntry(view, entries, 1, R.id.text_rank2_name, R.id.text_rank2_xp);
        bindPodiumEntry(view, entries, 2, R.id.text_rank3_name, R.id.text_rank3_xp);
    }

    private void bindPodiumEntry(View view, List<LeaderboardEntry> entries, int index,
                                 int nameViewId, int xpViewId) {
        if (entries.size() <= index) {
            return;
        }

        LeaderboardEntry entry = entries.get(index);
        ((TextView) view.findViewById(nameViewId)).setText(entry.getUsername());
        ((TextView) view.findViewById(xpViewId)).setText(entry.getFormattedXp());
    }

    private String resolveNextRankText(List<LeaderboardEntry> entries, LeaderboardEntry currentUser) {
        if (currentUser == null || currentUser.getRank() <= 1) {
            return "TOP RANK";
        }

        for (LeaderboardEntry entry : entries) {
            if (entry.getRank() == currentUser.getRank() - 1) {
                int xpGap = Math.max(entry.getXp() - currentUser.getXp(), 0);
                return "NEXT RANK: " + xpGap + " XP";
            }
        }

        return "NEXT RANK: --";
    }
}
