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
import com.arena.app.repository.LeaderboardRepository;
import com.arena.app.ui.profile.PublicProfileFragment;
import com.facebook.shimmer.ShimmerFrameLayout;
import android.widget.Toast;

public class LeaderboardFragment extends Fragment {

    private LeaderboardAdapter adapter;
    private ShimmerFrameLayout shimmer;
    private boolean shimmerStopped = false;
    private TextView tabFriends;
    private TextView tabSchool;
    private TextView tabGlobal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Start shimmer immediately
        shimmer = view.findViewById(R.id.shimmer_leaderboard);
        if (shimmer != null) shimmer.startShimmer();

        setupRecyclerView(view);
        setupActions(view);
        loadData(view);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_leaderboard);
        adapter = new LeaderboardAdapter();
        adapter.setListener(entry ->
                androidx.navigation.Navigation.findNavController(requireView()).navigate(
                        R.id.publicProfileFragment,
                        PublicProfileFragment.createArgs(entry)
                ));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        // Start hidden until shimmer stops
        recyclerView.setAlpha(0f);
    }

    private void setupActions(View view) {
        tabFriends = view.findViewById(R.id.tab_friends);
        tabSchool = view.findViewById(R.id.tab_school);
        tabGlobal = view.findViewById(R.id.tab_global);

        view.findViewById(R.id.btn_lb_back).setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigate(R.id.navigation_home));
        view.findViewById(R.id.btn_view_solution).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Solutions are coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_play_more).setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigate(R.id.navigation_arena));

        tabFriends.setOnClickListener(v -> selectTab(tabFriends, "Friends leaderboard is coming soon"));
        tabSchool.setOnClickListener(v -> selectTab(tabSchool, "College leaderboard is coming soon"));
        tabGlobal.setOnClickListener(v -> selectTab(tabGlobal, "Showing global leaderboard"));
    }

    private void selectTab(TextView selected, String message) {
        int inactive = requireContext().getColor(R.color.text_secondary);
        int active = requireContext().getColor(R.color.primary_blue);
        tabFriends.setTextColor(inactive);
        tabSchool.setTextColor(inactive);
        tabGlobal.setTextColor(inactive);
        selected.setTextColor(active);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadData(View view) {
        LeaderboardRepository repository = new LeaderboardRepository(requireContext());
        repository.getLeaderboard(null, 50).observe(getViewLifecycleOwner(), entries -> {
            if (entries == null || entries.isEmpty()) return;

            adapter.setItems(entries);
            stopShimmer(view);
        });
    }

    private void stopShimmer(View root) {
        if (shimmerStopped || shimmer == null) return;
        shimmerStopped = true;
        shimmer.stopShimmer();
        shimmer.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            shimmer.setVisibility(View.GONE);
            RecyclerView rv = root.findViewById(R.id.recycler_leaderboard);
            if (rv != null) rv.animate().alpha(1f).setDuration(400).start();
        }).start();
    }
}
