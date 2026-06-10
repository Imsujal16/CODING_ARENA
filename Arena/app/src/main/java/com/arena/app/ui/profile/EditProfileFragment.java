package com.arena.app.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.arena.app.MainActivity;
import com.arena.app.R;
import com.arena.app.auth.ClerkSessionBridge;
import com.arena.app.models.User;
import com.arena.app.repository.LocalProfileStore;
import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_USER_NAME = "user_name";
    private static final String ARG_USER_HANDLE = "user_handle";
    private static final String ARG_USER_EMAIL = "user_email";
    private static final String ARG_USER_AVATAR = "user_avatar";
    private static final String ARG_USER_BANNER = "user_banner";
    private static final String ARG_USER_AVATAR_COLOR = "user_avatar_color";
    private static final String ARG_USER_XP = "user_xp";
    private static final String ARG_USER_STREAK = "user_streak";
    private static final String ARG_USER_POINTS = "user_points";
    private static final String ARG_USER_BIO = "user_bio";
    private static final String ARG_USER_LOCATION = "user_location";
    private static final String ARG_USER_JOINED = "user_joined";
    private static final String ARG_USER_BIRTHDAY = "user_birthday";
    private static final String ARG_USER_FOLLOWING = "user_following";
    private static final String ARG_USER_FOLLOWERS = "user_followers";
    private static final String ARG_USER_VERIFIED = "user_verified";

    private LocalProfileStore localProfileStore;
    private User baseProfile;

    private TextInputEditText inputName;
    private TextInputEditText inputHandle;
    private TextInputEditText inputBio;
    private TextInputEditText inputLocation;
    private TextInputEditText inputJoined;
    private TextInputEditText inputBirthday;
    private TextInputEditText inputXp;
    private SwitchMaterial switchVerified;
    private TextView avatarPreview;
    private TextView previewName;
    private TextView previewHandle;
    private CircleImageView avatarImagePreview;
    private ImageView bannerImagePreview;
    private View[] colorSwatches;
    private String selectedAvatarColor = "#1D75D8";
    private String selectedAvatarUri;
    private String selectedBannerUri;
    private ActivityResultLauncher<String[]> avatarImagePicker;
    private ActivityResultLauncher<String[]> bannerImagePicker;

    private static final String[] AVATAR_COLORS = {
            "#1D75D8", "#16C7F3", "#A6FF6A", "#8B7CFF", "#F8A13B"
    };

    public static Bundle createArgs(User user) {
        Bundle args = new Bundle();
        if (user == null) {
            return args;
        }

        args.putString(ARG_USER_ID, user.getId());
        args.putString(ARG_USER_NAME, user.getUsername());
        args.putString(ARG_USER_HANDLE, user.getHandle());
        args.putString(ARG_USER_EMAIL, user.getEmail());
        args.putString(ARG_USER_AVATAR, user.getAvatarUrl());
        args.putString(ARG_USER_BANNER, user.getBannerUrl());
        args.putString(ARG_USER_AVATAR_COLOR, user.getAvatarColor());
        args.putInt(ARG_USER_XP, user.getXp());
        args.putInt(ARG_USER_STREAK, user.getStreak());
        args.putInt(ARG_USER_POINTS, user.getPoints());
        args.putString(ARG_USER_BIO, user.getBio());
        args.putString(ARG_USER_LOCATION, user.getLocation());
        args.putString(ARG_USER_JOINED, user.getJoinedDate());
        args.putString(ARG_USER_BIRTHDAY, user.getBirthday());
        args.putInt(ARG_USER_FOLLOWING, user.getFollowing());
        args.putInt(ARG_USER_FOLLOWERS, user.getFollowers());
        args.putBoolean(ARG_USER_VERIFIED, user.isVerified());
        return args;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        avatarImagePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) {
                return;
            }
            persistImagePermission(uri);
            selectedAvatarUri = uri.toString();
            bindPreview();
        });
        bannerImagePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) {
                return;
            }
            persistImagePermission(uri);
            selectedBannerUri = uri.toString();
            bindPreview();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        localProfileStore = new LocalProfileStore(requireContext());
        baseProfile = readProfileArgs();

        bindViews(view);
        bindProfile(baseProfile);
        setupActions(view);
    }

    private void bindViews(View view) {
        inputName = view.findViewById(R.id.input_profile_name);
        inputHandle = view.findViewById(R.id.input_profile_handle);
        inputBio = view.findViewById(R.id.input_profile_bio);
        inputLocation = view.findViewById(R.id.input_profile_location);
        inputJoined = view.findViewById(R.id.input_profile_joined);
        inputBirthday = view.findViewById(R.id.input_profile_birthday);
        inputXp = view.findViewById(R.id.input_profile_xp);
        switchVerified = view.findViewById(R.id.switch_profile_verified);
        avatarPreview = view.findViewById(R.id.text_edit_avatar_initial);
        previewName = view.findViewById(R.id.text_edit_preview_name);
        previewHandle = view.findViewById(R.id.text_edit_preview_handle);
        avatarImagePreview = view.findViewById(R.id.img_edit_avatar_preview);
        bannerImagePreview = view.findViewById(R.id.img_edit_banner_preview);
        colorSwatches = new View[] {
                view.findViewById(R.id.swatch_green),
                view.findViewById(R.id.swatch_purple),
                view.findViewById(R.id.swatch_orange),
                view.findViewById(R.id.swatch_blue),
                view.findViewById(R.id.swatch_red)
        };
    }

    private void bindProfile(User user) {
        selectedAvatarColor = readTextOrFallback(user.getAvatarColor(), "#1D75D8");
        selectedAvatarUri = user.getAvatarUrl();
        selectedBannerUri = user.getBannerUrl();
        setText(inputName, user.getUsername());
        setText(inputHandle, user.getHandle());
        setText(inputBio, user.getBio());
        setText(inputLocation, user.getLocation());
        setText(inputJoined, user.getJoinedDate());
        setText(inputBirthday, user.getBirthday());
        setText(inputXp, String.valueOf(user.getXp()));
        switchVerified.setChecked(user.isVerified());
        bindPreview();
        bindSwatches();
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btn_edit_profile_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.btn_cancel_profile).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveProfile());
        view.findViewById(R.id.btn_logout_edit_profile).setOnClickListener(v -> showSignOutDialog());
        view.findViewById(R.id.btn_change_avatar).setOnClickListener(v ->
                avatarImagePicker.launch(new String[]{"image/*"}));
        view.findViewById(R.id.btn_change_banner).setOnClickListener(v ->
                bannerImagePicker.launch(new String[]{"image/*"}));
        inputName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                bindPreview();
            }
        });
        inputHandle.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                bindPreview();
            }
        });
        for (int i = 0; i < colorSwatches.length; i++) {
            final int index = i;
            colorSwatches[i].setOnClickListener(v -> {
                selectedAvatarColor = AVATAR_COLORS[index];
                bindPreview();
                bindSwatches();
            });
        }
    }

    private void saveProfile() {
        String username = readText(inputName);
        if (!hasText(username)) {
            inputName.setError(getString(R.string.profile_username_required));
            Toast.makeText(requireContext(), R.string.profile_username_required, Toast.LENGTH_SHORT).show();
            return;
        }

        inputName.setError(null);

        User updated = new User();
        updated.setId(baseProfile.getId());
        updated.setAvatarUrl(selectedAvatarUri);
        updated.setBannerUrl(selectedBannerUri);
        updated.setAvatarColor(selectedAvatarColor);
        updated.setUsername(username);
        updated.setHandle(normalizeHandle(readText(inputHandle), username));
        updated.setEmail(baseProfile.getEmail());
        updated.setBio(readText(inputBio));
        updated.setLocation(readText(inputLocation));
        updated.setJoinedDate(readText(inputJoined));
        updated.setBirthday(readText(inputBirthday));
        updated.setXp(parseInt(readText(inputXp), baseProfile.getXp()));
        updated.setVerified(switchVerified.isChecked());

        localProfileStore.saveProfile(updated);
        Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
        if (getView() != null) {
            Navigation.findNavController(getView()).navigateUp();
        }
    }

    private User readProfileArgs() {
        User fallback = localProfileStore.getEffectiveProfile(localProfileStore.getDefaultProfile());
        Bundle args = getArguments();
        if (args == null) {
            return fallback;
        }

        User user = new User();
        user.setId(readTextOrFallback(args.getString(ARG_USER_ID), fallback.getId()));
        user.setUsername(readTextOrFallback(args.getString(ARG_USER_NAME), fallback.getUsername()));
        user.setHandle(readTextOrFallback(args.getString(ARG_USER_HANDLE), fallback.getHandle()));
        user.setEmail(readTextOrFallback(args.getString(ARG_USER_EMAIL), fallback.getEmail()));
        user.setAvatarUrl(readTextOrFallback(args.getString(ARG_USER_AVATAR), fallback.getAvatarUrl()));
        user.setBannerUrl(readTextOrFallback(args.getString(ARG_USER_BANNER), fallback.getBannerUrl()));
        user.setAvatarColor(readTextOrFallback(args.getString(ARG_USER_AVATAR_COLOR), fallback.getAvatarColor()));
        user.setXp(args.getInt(ARG_USER_XP, fallback.getXp()));
        user.setStreak(args.getInt(ARG_USER_STREAK, fallback.getStreak()));
        user.setPoints(args.getInt(ARG_USER_POINTS, fallback.getPoints()));
        user.setBio(readTextOrFallback(args.getString(ARG_USER_BIO), fallback.getBio()));
        user.setLocation(readTextOrFallback(args.getString(ARG_USER_LOCATION), fallback.getLocation()));
        user.setJoinedDate(readTextOrFallback(args.getString(ARG_USER_JOINED), fallback.getJoinedDate()));
        user.setBirthday(readTextOrFallback(args.getString(ARG_USER_BIRTHDAY), fallback.getBirthday()));
        user.setFollowing(args.getInt(ARG_USER_FOLLOWING, fallback.getFollowing()));
        user.setFollowers(args.getInt(ARG_USER_FOLLOWERS, fallback.getFollowers()));
        user.setVerified(args.getBoolean(ARG_USER_VERIFIED, fallback.isVerified()));
        return user;
    }

    private void bindPreview() {
        String username = readTextOrFallback(inputName, "Coder");
        String handle = normalizeHandle(readText(inputHandle), username);

        avatarPreview.setText(String.valueOf(username.charAt(0)).toUpperCase());
        avatarPreview.setBackground(makeCircle(selectedAvatarColor, 0, 0));
        previewName.setText(username);
        previewHandle.setText(handle);
        bindAvatarImagePreview();
        bindBannerImagePreview();
    }

    private void bindAvatarImagePreview() {
        if (hasText(selectedAvatarUri)) {
            avatarPreview.setVisibility(View.GONE);
            Glide.with(this)
                    .load(Uri.parse(selectedAvatarUri))
                    .centerCrop()
                    .into(avatarImagePreview);
            return;
        }

        avatarImagePreview.setImageDrawable(null);
        avatarImagePreview.setCircleBackgroundColor(parseColor(selectedAvatarColor, Color.parseColor("#1D75D8")));
        avatarPreview.setVisibility(View.VISIBLE);
    }

    private void bindBannerImagePreview() {
        if (hasText(selectedBannerUri)) {
            Glide.with(this)
                    .load(Uri.parse(selectedBannerUri))
                    .centerCrop()
                    .into(bannerImagePreview);
            return;
        }

        bannerImagePreview.setImageDrawable(null);
        bannerImagePreview.setBackgroundColor(parseColor(selectedAvatarColor, Color.parseColor("#1D75D8")));
    }

    private void bindSwatches() {
        for (int i = 0; i < colorSwatches.length; i++) {
            boolean selected = AVATAR_COLORS[i].equalsIgnoreCase(selectedAvatarColor);
            colorSwatches[i].setBackground(makeCircle(
                    AVATAR_COLORS[i],
                    selected ? Color.parseColor("#171A1F") : Color.parseColor("#DDE2E8"),
                    selected ? 4 : 1
            ));
            colorSwatches[i].setSelected(selected);
        }
    }

    private GradientDrawable makeCircle(String fillColor, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(parseColor(fillColor, Color.parseColor("#1D75D8")));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private void persistImagePermission(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Some providers grant a regular read URI instead of a persistable one.
        }
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

    private void setText(TextInputEditText input, String value) {
        input.setText(value == null ? "" : value);
    }

    private String readText(TextInputEditText input) {
        if (input == null || input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private String readTextOrFallback(TextInputEditText input, String fallback) {
        String value = readText(input);
        return hasText(value) ? value : fallback;
    }

    private String readTextOrFallback(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private int parseColor(String value, int fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int parseInt(String value, int fallback) {
        if (!hasText(value)) {
            return fallback;
        }

        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String normalizeHandle(String handle, String username) {
        String value = hasText(handle) ? handle.trim() : username.trim().replace(" ", "").toLowerCase();
        return value.startsWith("@") ? value : "@" + value;
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
