package com.arena.app.ui.learn;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.adapters.RoadmapDetailAdapter;
import com.arena.app.utils.LocalProgressStore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RoadmapDetailFragment extends Fragment {

    private RoadmapDetailAdapter adapter;
    private LocalProgressStore progressStore;
    private ProgressBar progressBar;
    private TextView textProgress;
    private List<RoadmapDetailAdapter.StriverProblem> allProblems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_roadmap_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressStore = new LocalProgressStore(requireContext());
        
        view.findViewById(R.id.btn_back).setOnClickListener(v -> 
                Navigation.findNavController(view).navigateUp());

        progressBar = view.findViewById(R.id.progress_roadmap);
        textProgress = view.findViewById(R.id.text_roadmap_progress);

        RecyclerView recycler = view.findViewById(R.id.recycler_roadmap_problems);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RoadmapDetailAdapter();
        recycler.setAdapter(adapter);

        adapter.setListener(new RoadmapDetailAdapter.OnProblemInteractionListener() {
            @Override
            public void onProblemChecked(RoadmapDetailAdapter.StriverProblem problem, boolean isChecked) {
                progressStore.setProblemSolved(problem.slug, isChecked);
                updateProgress();
            }

            @Override
            public void onProblemClicked(RoadmapDetailAdapter.StriverProblem problem) {
                if (problem.url != null && !problem.url.isEmpty()) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(problem.url));
                    startActivity(browserIntent);
                }
            }
        });

        loadStriverData();
    }

    private void loadStriverData() {
        try {
            InputStream is = requireContext().getAssets().open("striver_a2z.json");
            Gson gson = new Gson();
            Type listType = new TypeToken<List<RoadmapDetailAdapter.StriverProblem>>() {}.getType();
            allProblems = gson.fromJson(new InputStreamReader(is), listType);

            String currentTopic = "";
            for (RoadmapDetailAdapter.StriverProblem p : allProblems) {
                p.isSolved = progressStore.isProblemSolved(p.slug);
                if (!p.topic.equals(currentTopic)) {
                    p.isFirstInTopic = true;
                    currentTopic = p.topic;
                } else {
                    p.isFirstInTopic = false;
                }
            }

            adapter.setItems(allProblems);
            updateProgress();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateProgress() {
        if (allProblems.isEmpty()) return;
        
        int solved = 0;
        for (RoadmapDetailAdapter.StriverProblem p : allProblems) {
            if (p.isSolved) solved++;
        }
        
        int percent = (int) (((float) solved / allProblems.size()) * 100);
        progressBar.setProgress(percent);
        textProgress.setText(percent + "%");
    }
}
