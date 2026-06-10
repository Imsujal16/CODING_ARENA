package com.arena.app.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.shimmer.ShimmerFrameLayout;

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
import com.arena.app.ui.solver.ProblemSolverFragment;
import com.arena.app.utils.ClerkAuthHelper;
import com.bumptech.glide.Glide;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private RecentActivityAdapter activityAdapter;
    private LocalProfileStore localProfileStore;
    private User currentUser;
    private ShimmerFrameLayout shimmerProfile;
    private boolean shimmerStopped = false;

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

        shimmerProfile = view.findViewById(R.id.shimmer_profile);
        if (shimmerProfile != null) shimmerProfile.startShimmer();

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
        activityAdapter.setListener(problem ->
                Navigation.findNavController(requireView()).navigate(
                        R.id.problemSolverFragment,
                        ProblemSolverFragment.createArgs(problem)
                ));
    }

    private void loadData(View view) {
        UserRepository userRepository = new UserRepository(requireContext());
        ProblemRepository problemRepository = new ProblemRepository(requireContext());

        userRepository.getUserProfile().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            bindProfile(view, localProfileStore.getEffectiveProfile(user));
            stopShimmer(view);
        });
        problemRepository.getRecentActivity().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                activityAdapter.setItems(list);
            }
        });

        view.findViewById(R.id.btn_share).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_home));
        view.findViewById(R.id.text_profile_share_pill).setOnClickListener(v -> shareProfile());
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

    private void shareProfile() {
        String username = currentUser == null
                ? localProfileStore.getDefaultProfile().getUsername()
                : localProfileStore.getEffectiveProfile(currentUser).getUsername();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,
                "Check out " + safe(username, "my") + " Arena profile.");
        startActivity(Intent.createChooser(intent, "Share profile"));
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
        ((TextView) view.findViewById(R.id.text_profile_banner_rank))
                .setText(safe(user.getRank(), "Master").toUpperCase());

        TextView avatarInitial = view.findViewById(R.id.text_profile_avatar_initial);
        avatarInitial.setText(String.valueOf(username.charAt(0)).toUpperCase());
        CircleImageView avatar = view.findViewById(R.id.img_profile_avatar);
        int avatarColor = parseColor(user.getAvatarColor(), Color.parseColor("#1D75D8"));
        bindBanner(view, user, avatarColor);
        bindAvatar(user, avatar, avatarInitial, avatarColor);

        view.findViewById(R.id.icon_verified)
                .setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
    }

    private void bindBanner(View view, User user, int fallbackColor) {
        ImageView banner = view.findViewById(R.id.img_banner);
        if (hasText(user.getBannerUrl())) {
            Glide.with(this)
                    .load(Uri.parse(user.getBannerUrl()))
                    .centerCrop()
                    .into(banner);
            return;
        }

        banner.setImageDrawable(null);
        banner.setBackgroundResource(R.drawable.bg_profile_banner_default);
    }

    private void bindAvatar(User user, CircleImageView avatar, TextView avatarInitial, int fallbackColor) {
        if (hasText(user.getAvatarUrl())) {
            avatarInitial.setVisibility(View.GONE);
            Glide.with(this)
                    .load(Uri.parse(user.getAvatarUrl()))
                    .centerCrop()
                    .into(avatar);
            return;
        }

        avatar.setImageDrawable(null);
        avatar.setCircleBackgroundColor(fallbackColor);
        avatarInitial.setVisibility(View.VISIBLE);
    }

    private int parseColor(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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

    private void stopShimmer(View root) {
        if (shimmerStopped || shimmerProfile == null) return;
        shimmerStopped = true;
        shimmerProfile.stopShimmer();
        shimmerProfile.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            shimmerProfile.setVisibility(View.GONE);
            View info = root.findViewById(R.id.layout_info);
            if (info != null) info.animate().alpha(1f).setDuration(400).start();
        }).start();
    }
}
