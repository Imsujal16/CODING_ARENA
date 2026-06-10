package com.arena.app.ui.profile;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arena.app.R;
import com.arena.app.models.LeaderboardEntry;
import com.arena.app.models.User;
import com.arena.app.repository.LocalProfileStore;
import com.bumptech.glide.Glide;

import de.hdodenhof.circleimageview.CircleImageView;

public class PublicProfileFragment extends Fragment {
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_AVATAR_URL = "avatar_url";
    private static final String ARG_XP = "xp";
    private static final String ARG_RANK = "rank";
    private static final String ARG_STREAK = "streak";
    private static final String ARG_SOLVED = "solved";
    private static final String ARG_LEAGUE = "league";
    private static final String ARG_IS_CURRENT_USER = "is_current_user";

    public static Bundle createArgs(LeaderboardEntry entry) {
        Bundle args = new Bundle();
        if (entry == null) {
            return args;
        }
        args.putString(ARG_USER_ID, entry.getUserId());
        args.putString(ARG_USERNAME, entry.getUsername());
        args.putString(ARG_AVATAR_URL, entry.getAvatarUrl());
        args.putInt(ARG_XP, entry.getXp());
        args.putInt(ARG_RANK, entry.getRank());
        args.putInt(ARG_STREAK, entry.getCurrentStreak());
        args.putInt(ARG_SOLVED, entry.getTotalSolved());
        args.putString(ARG_LEAGUE, entry.getLeagueTier());
        args.putBoolean(ARG_IS_CURRENT_USER, entry.isCurrentUser());
        return args;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btn_public_profile_back).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
        bindProfile(view);
    }

    private void bindProfile(View view) {
        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        String userId = args.getString(ARG_USER_ID, "");
        String username = safe(args.getString(ARG_USERNAME), "Arena Player");
        String avatarUrl = args.getString(ARG_AVATAR_URL, "");
        String bannerUrl = "";
        String bio = "This player is climbing the Arena leaderboard.";
        String handle = "@" + username.replace(" ", "").toLowerCase();
        int points = args.getInt(ARG_XP, 0);
        int streak = args.getInt(ARG_STREAK, 0);
        int solved = args.getInt(ARG_SOLVED, 0);
        String league = safe(args.getString(ARG_LEAGUE), "GLOBAL");
        boolean isCurrentUser = args.getBoolean(ARG_IS_CURRENT_USER, false);

        if (isCurrentUser || hasText(userId)) {
            User savedProfile = new LocalProfileStore(requireContext()).getSavedProfileForUserId(userId);
            if (savedProfile != null) {
                username = safe(savedProfile.getUsername(), username);
                handle = safe(savedProfile.getHandle(), handle);
                avatarUrl = safe(savedProfile.getAvatarUrl(), avatarUrl);
                bannerUrl = safe(savedProfile.getBannerUrl(), "");
                bio = safe(savedProfile.getBio(), bio);
                points = savedProfile.getXp() > 0 ? savedProfile.getXp() : points;
                streak = savedProfile.getStreak() > 0 ? savedProfile.getStreak() : streak;
                league = safe(savedProfile.getRank(), league);
            }
        }

        ((TextView) view.findViewById(R.id.text_public_name)).setText(username);
        ((TextView) view.findViewById(R.id.text_public_handle)).setText(handle);
        ((TextView) view.findViewById(R.id.text_public_subtitle)).setText("Rank #" + args.getInt(ARG_RANK, 0) + " on the leaderboard");
        ((TextView) view.findViewById(R.id.text_public_rank)).setText(league.toUpperCase());
        ((TextView) view.findViewById(R.id.text_public_xp)).setText(formatNumber(points) + "\nXP");
        ((TextView) view.findViewById(R.id.text_public_streak)).setText(streak + "\nSTREAK");
        ((TextView) view.findViewById(R.id.text_public_solved)).setText(solved + "\nSOLVED");
        ((TextView) view.findViewById(R.id.text_public_league)).setText(league.toUpperCase());
        ((TextView) view.findViewById(R.id.text_public_bio)).setText(bio);

        bindBanner(view.findViewById(R.id.img_public_banner), bannerUrl);
        bindAvatar(view.findViewById(R.id.img_public_avatar),
                view.findViewById(R.id.text_public_avatar_initial),
                username,
                avatarUrl,
                userId);
    }

    private void bindBanner(ImageView banner, String bannerUrl) {
        if (hasText(bannerUrl)) {
            Glide.with(this).load(Uri.parse(bannerUrl)).centerCrop().into(banner);
            return;
        }
        banner.setImageDrawable(null);
        banner.setBackgroundResource(R.drawable.bg_profile_banner_default);
    }

    private void bindAvatar(CircleImageView avatar, TextView initial, String username, String avatarUrl, String userId) {
        initial.setText(String.valueOf(safe(username, "A").charAt(0)).toUpperCase());
        if (hasText(avatarUrl)) {
            initial.setVisibility(View.GONE);
            Glide.with(this)
                    .load(Uri.parse(avatarUrl))
                    .centerCrop()
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .into(avatar);
            return;
        }
        avatar.setImageDrawable(null);
        avatar.setCircleBackgroundColor(resolveAvatarColor(hasText(userId) ? userId : username));
        initial.setVisibility(View.VISIBLE);
    }

    private int resolveAvatarColor(String key) {
        int[] colors = {
                Color.parseColor("#1D75D8"),
                Color.parseColor("#16C7F3"),
                Color.parseColor("#8B7CFF"),
                Color.parseColor("#3FD6C1")
        };
        return colors[Math.abs(safe(key, "arena").hashCode()) % colors.length];
    }

    private String formatNumber(int value) {
        return String.format("%,d", value);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
