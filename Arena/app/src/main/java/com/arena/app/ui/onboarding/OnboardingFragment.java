package com.arena.app.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.arena.app.R;
import com.arena.app.auth.ClerkAuthActivity;
import com.arena.app.utils.ClerkAuthHelper;
import com.google.android.material.button.MaterialButton;

public class OnboardingFragment extends Fragment {
    private ActivityResultLauncher<Intent> authLauncher;
    private boolean autoLaunchAttempted;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            || result.getResultCode() == ClerkAuthActivity.RESULT_CONTINUE_AS_GUEST) {
                        completeOnboardingAndNavigate();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Check if onboarding already completed
        ClerkAuthHelper authHelper = new ClerkAuthHelper(requireContext());
        if (authHelper.isOnboardingCompleted()
                && (authHelper.isLoggedIn() || authHelper.isGuestModeEnabled())) {
            // Navigate directly to home
            View tempView = inflater.inflate(R.layout.fragment_onboarding, container, false);
            tempView.post(() -> {
                try {
                    Navigation.findNavController(tempView)
                            .navigate(R.id.action_onboarding_to_home);
                } catch (Exception ignored) {}
            });
            return tempView;
        }

        return inflater.inflate(R.layout.fragment_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnGetStarted = view.findViewById(R.id.btn_get_started);
        ClerkAuthHelper authHelper = new ClerkAuthHelper(requireContext());

        // Animate elements in
        animateViewsIn(view);

        btnGetStarted.setOnClickListener(v -> {
            launchAuth();
        });

        if (authHelper.isOnboardingCompleted()
                && !authHelper.isLoggedIn()
                && !authHelper.isGuestModeEnabled()
                && !autoLaunchAttempted) {
            autoLaunchAttempted = true;
            view.post(this::launchAuth);
        }
    }

    private void animateViewsIn(View root) {
        View gradientArea = root.findViewById(R.id.gradient_area);
        View textWelcome = root.findViewById(R.id.text_welcome);
        View textArena = root.findViewById(R.id.text_arena);
        View textHeyCoder = root.findViewById(R.id.text_hey_coder);
        View textDesc = root.findViewById(R.id.text_description);
        View dots = root.findViewById(R.id.dot_indicators);
        View btn = root.findViewById(R.id.btn_get_started);

        View[] views = {gradientArea, textWelcome, textArena, textHeyCoder, textDesc, dots, btn};

        for (int i = 0; i < views.length; i++) {
            views[i].setAlpha(0f);
            views[i].setTranslationY(30f);
            views[i].animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(i * 100L)
                    .start();
        }
    }

    private void launchAuth() {
        if (authLauncher == null || getContext() == null) {
            return;
        }

        authLauncher.launch(new Intent(requireContext(), ClerkAuthActivity.class));
    }

    private void completeOnboardingAndNavigate() {
        if (getView() == null) {
            return;
        }

        ClerkAuthHelper authHelper = new ClerkAuthHelper(requireContext());
        authHelper.setOnboardingCompleted(true);

        try {
            Navigation.findNavController(requireView()).navigate(R.id.action_onboarding_to_home);
        } catch (Exception ignored) {
        }
    }
}
