package com.arena.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.MainActivity;
import com.arena.app.R;
import com.arena.app.adapters.RecentActivityAdapter;
import com.arena.app.auth.ClerkSessionBridge;
import com.arena.app.models.User;
import com.arena.app.repository.LocalProfileStore;
import com.arena.app.repository.ProblemRepository;
import com.arena.app.repository.UserRepository;
import com.arena.app.utils.ClerkAuthHelper;

public class ProfileFragment extends Fragment {

    private RecentActivityAdapter activityAdapter;
    private LocalProfileStore localProfileStore;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        localProfileStore = new LocalProfileStore(requireContext());

        setupRecyclerView(view);
        loadData(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && currentUser != null) {
            bindProfile(getView(), localProfileStore.getEffectiveProfile(currentUser));
        }
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_recent_activity);
        activityAdapter = new RecentActivityAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(activityAdapter);
    }

    private void loadData(View view) {
        UserRepository userRepository = new UserRepository(requireContext());
        ProblemRepository problemRepository = new ProblemRepository(requireContext());

        userRepository.getUserProfile().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            bindProfile(view, localProfileStore.getEffectiveProfile(user));
        });
        problemRepository.getRecentActivity().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                activityAdapter.setItems(list);
            }
        });

        view.findViewById(R.id.btn_profile_settings).setOnClickListener(v -> showSignOutDialog());
        view.findViewById(R.id.btn_logout_profile).setOnClickListener(v -> showSignOutDialog());
        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            User profileToEdit = currentUser == null
                    ? localProfileStore.getEffectiveProfile(localProfileStore.getDefaultProfile())
                    : localProfileStore.getEffectiveProfile(currentUser);
            Navigation.findNavController(v).navigate(
                    R.id.editProfileFragment,
                    EditProfileFragment.createArgs(profileToEdit)
            );
        });
    }

    private void bindProfile(View view, User user) {
        if (user == null) {
            return;
        }

        boolean isGuestMode = new ClerkAuthHelper(requireContext()).isGuestModeEnabled();
        String username = safe(user.getUsername(), isGuestMode ? "Guest" : "Coder");
        String handle = safe(user.getHandle(), isGuestMode ? "" : "@" + username.replace(" ", "").toLowerCase());

        ((TextView) view.findViewById(R.id.text_profile_name)).setText(username);
        setOptionalText(view.findViewById(R.id.text_handle), handle, null);
        setOptionalText(view.findViewById(R.id.text_email), user.getEmail(), null);
        setOptionalText(view.findViewById(R.id.text_bio), user.getBio(), null);
        setOptionalText(view.findViewById(R.id.text_location), user.getLocation(), "\uD83D\uDCCD ");
        setOptionalText(view.findViewById(R.id.text_joined), user.getJoinedDate(), "\uD83D\uDCC5 ");
        setOptionalText(view.findViewById(R.id.text_birthday), user.getBirthday(), "\uD83C\uDF82 ");

        ((TextView) view.findViewById(R.id.text_points_value)).setText(user.getFormattedPoints());
        ((TextView) view.findViewById(R.id.text_streak_value)).setText(String.valueOf(user.getStreak()));

        TextView avatarInitial = view.findViewById(R.id.text_profile_avatar_initial);
        avatarInitial.setText(String.valueOf(username.charAt(0)));

        view.findViewById(R.id.icon_verified)
                .setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void setOptionalText(TextView view, String value, @Nullable String prefix) {
        if (view == null) {
            return;
        }

        if (value == null || value.trim().isEmpty()) {
            view.setText("");
            view.setVisibility(View.GONE);
            return;
        }

        view.setVisibility(View.VISIBLE);
        view.setText(prefix == null ? value.trim() : prefix + value.trim());
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sign_out_confirm, (dialog, which) ->
                        ClerkSessionBridge.signOut(requireContext(), () -> {
                            Intent intent = new Intent(requireContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        }))
                .show();
    }
}
