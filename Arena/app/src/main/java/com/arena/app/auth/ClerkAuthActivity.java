package com.arena.app.auth;

import android.app.Activity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.arena.app.R;
import com.arena.app.models.User;
import com.arena.app.repository.LocalProfileStore;
import com.arena.app.utils.ClerkAuthHelper;
import com.clerk.Clerk;
import com.clerk.emailaddress.EmailAddress;
import com.clerk.network.model.error.ClerkErrorResponse;
import com.clerk.network.serialization.ClerkResult;
import com.clerk.signin.SignIn;
import com.clerk.signup.SignUp;
import com.clerk.signup.SignUpKt;
import com.clerk.sso.sso.OAuthResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.ResultKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

public class ClerkAuthActivity extends AppCompatActivity {
    public static final int RESULT_CONTINUE_AS_GUEST = Activity.RESULT_FIRST_USER;
    private static final int MODE_SIGN_IN = 0;
    private static final int MODE_SIGN_UP = 1;
    private static final int MODE_VERIFY = 2;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private MaterialButton signInTabButton;
    private MaterialButton signUpTabButton;
    private MaterialButton googleButton;
    private MaterialButton continueGuestButton;
    private MaterialButton signInButton;
    private MaterialButton signUpButton;
    private MaterialButton verifyButton;
    private MaterialButton resendCodeButton;
    private ProgressBar progressBar;
    private TextView titleText;
    private TextView subtitleText;
    private TextView statusText;
    private View signInContainer;
    private View signUpContainer;
    private View verifyContainer;

    private TextInputEditText signInIdentifierInput;
    private TextInputEditText signInPasswordInput;
    private TextInputEditText signUpNameInput;
    private TextInputEditText signUpUsernameInput;
    private TextInputEditText signUpEmailInput;
    private TextInputEditText signUpPasswordInput;
    private TextInputEditText signUpBioInput;
    private TextInputEditText signUpLocationInput;
    private TextInputEditText signUpBirthdayInput;
    private TextInputEditText verificationCodeInput;
    private TextInputLayout signUpEmailLayout;
    private TextInputLayout signUpPasswordLayout;

    @Nullable
    private SignUp pendingVerificationSignUp;
    @Nullable
    private SignUp pendingGoogleCompletionSignUp;
    private int currentMode = MODE_SIGN_IN;
    private boolean shouldPersistSignUpProfile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClerkSessionBridge.ensureInitialized(this);

        if (ClerkSessionBridge.isSignedIn(this)) {
            finishWithSuccess();
            return;
        }

        setContentView(R.layout.activity_clerk_auth);
        bindViews();
        bindActions();
        switchMode(MODE_SIGN_IN);
    }

    @Override
    public void onBackPressed() {
        if (currentMode == MODE_VERIFY) {
            switchMode(MODE_SIGN_UP);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void bindViews() {
        titleText = findViewById(R.id.text_auth_title);
        subtitleText = findViewById(R.id.text_auth_subtitle);
        statusText = findViewById(R.id.text_auth_status);
        progressBar = findViewById(R.id.progress_auth);

        signInTabButton = findViewById(R.id.btn_tab_sign_in);
        signUpTabButton = findViewById(R.id.btn_tab_sign_up);
        googleButton = findViewById(R.id.btn_google_auth);
        continueGuestButton = findViewById(R.id.btn_continue_guest);
        signInButton = findViewById(R.id.btn_sign_in_submit);
        signUpButton = findViewById(R.id.btn_sign_up_submit);
        verifyButton = findViewById(R.id.btn_verify_submit);
        resendCodeButton = findViewById(R.id.btn_resend_code);

        signInContainer = findViewById(R.id.layout_sign_in_form);
        signUpContainer = findViewById(R.id.layout_sign_up_form);
        verifyContainer = findViewById(R.id.layout_verify_form);

        signInIdentifierInput = findViewById(R.id.input_sign_in_identifier);
        signInPasswordInput = findViewById(R.id.input_sign_in_password);
        signUpNameInput = findViewById(R.id.input_sign_up_name);
        signUpUsernameInput = findViewById(R.id.input_sign_up_username);
        signUpEmailInput = findViewById(R.id.input_sign_up_email);
        signUpPasswordInput = findViewById(R.id.input_sign_up_password);
        signUpBioInput = findViewById(R.id.input_sign_up_bio);
        signUpLocationInput = findViewById(R.id.input_sign_up_location);
        signUpBirthdayInput = findViewById(R.id.input_sign_up_birthday);
        verificationCodeInput = findViewById(R.id.input_verification_code);
        signUpEmailLayout = findViewById(R.id.layout_sign_up_email);
        signUpPasswordLayout = findViewById(R.id.layout_sign_up_password);
    }

    private void bindActions() {
        signInTabButton.setOnClickListener(v -> {
            clearPendingGoogleCompletion();
            clearSignUpProfileDraft();
            switchMode(MODE_SIGN_IN);
        });
        signUpTabButton.setOnClickListener(v -> {
            clearPendingGoogleCompletion();
            switchMode(MODE_SIGN_UP);
        });
        googleButton.setOnClickListener(v -> startGoogleAuthentication());
        continueGuestButton.setOnClickListener(v -> finishAsGuest());
        signInButton.setOnClickListener(v -> submitSignIn());
        signUpButton.setOnClickListener(v -> submitSignUp());
        verifyButton.setOnClickListener(v -> submitVerification());
        resendCodeButton.setOnClickListener(v -> resendVerificationCode());
    }

    private void switchMode(int mode) {
        currentMode = mode;
        clearStatus();

        signInContainer.setVisibility(mode == MODE_SIGN_IN ? View.VISIBLE : View.GONE);
        signUpContainer.setVisibility(mode == MODE_SIGN_UP ? View.VISIBLE : View.GONE);
        verifyContainer.setVisibility(mode == MODE_VERIFY ? View.VISIBLE : View.GONE);
        googleButton.setVisibility(mode == MODE_VERIFY ? View.GONE : View.VISIBLE);

        if (mode == MODE_SIGN_IN) {
            titleText.setText(R.string.auth_sign_in_title);
            subtitleText.setText(R.string.auth_sign_in_subtitle);
        } else if (mode == MODE_SIGN_UP) {
            if (pendingGoogleCompletionSignUp != null) {
                titleText.setText(R.string.auth_google_complete_title);
                subtitleText.setText(R.string.auth_google_complete_subtitle);
            } else {
                titleText.setText(R.string.auth_sign_up_title);
                subtitleText.setText(R.string.auth_sign_up_subtitle);
            }
        } else {
            titleText.setText(R.string.auth_verify_title);
            String email = pendingVerificationSignUp != null
                    ? safeText(pendingVerificationSignUp.getEmailAddress())
                    : "";
            subtitleText.setText(getString(R.string.auth_verify_subtitle, email));
        }

        updateSignUpFormState();
        updateTabStyles(mode);
    }

    private void updateTabStyles(int mode) {
        applyTabStyle(signInTabButton, mode == MODE_SIGN_IN);
        applyTabStyle(signUpTabButton, mode != MODE_SIGN_IN);
    }

    private void applyTabStyle(MaterialButton button, boolean active) {
        int textColor = ContextCompat.getColor(this, active ? R.color.text_white : R.color.primary_blue);

        button.setBackgroundTintList(ContextCompat.getColorStateList(this, active ? R.color.primary_blue : R.color.card_background));
        button.setTextColor(textColor);
        button.setStrokeColor(ContextCompat.getColorStateList(this, R.color.primary_blue));
        button.setStrokeWidth(active ? 0 : 2);
        button.setRippleColor(ContextCompat.getColorStateList(this, R.color.primary_blue_light));
    }

    private void submitSignIn() {
        clearSignUpProfileDraft();
        String identifier = safeText(signInIdentifierInput.getText());
        String password = safeText(signInPasswordInput.getText());

        if (identifier.isEmpty()) {
            showError(getString(R.string.auth_identifier_required));
            return;
        }
        if (password.isEmpty()) {
            showError(getString(R.string.auth_password_required));
            return;
        }

        setLoading(true);
        executor.execute(() -> {
            try {
                ClerkResult<SignIn, ClerkErrorResponse> result = ClerkSuspendUtils.await(
                        continuation -> SignIn.Companion.create(
                                new SignIn.CreateParams.Strategy.Password(identifier, password, "password"),
                                continuation
                        )
                );
                runOnUiThread(() -> handleSignInResult(result));
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(resolveThrowableMessage(exception));
                });
            }
        });
    }

    private void handleSignInResult(ClerkResult<SignIn, ClerkErrorResponse> result) {
        setLoading(false);

        if (result instanceof ClerkResult.Success) {
            SignIn signIn = ((ClerkResult.Success<SignIn>) result).getValue();
            if (signIn != null && signIn.getStatus() == SignIn.Status.COMPLETE) {
                finishWithSuccess();
                return;
            }

            showError(getString(R.string.auth_sign_in_additional_factor));
            return;
        }

        showError(getFailureMessage((ClerkResult.Failure<ClerkErrorResponse>) result));
    }

    private void startGoogleAuthentication() {
        clearPendingGoogleCompletion();
        clearSignUpProfileDraft();
        ClerkSessionBridge.ensureInitialized(this);
        setLoading(true);
        executeSuspendCall(
                continuation -> SignIn.Companion.authenticateWithGoogle(continuation),
                this::handleGoogleAuthenticationResult
        );
    }

    private void handleGoogleAuthenticationResult(ClerkResult<OAuthResult, ClerkErrorResponse> result) {
        setLoading(false);

        if (result instanceof ClerkResult.Success) {
            OAuthResult oAuthResult = ((ClerkResult.Success<OAuthResult>) result).getValue();
            if (oAuthResult == null) {
                showError(getString(R.string.auth_google_incomplete));
                return;
            }

            SignIn signIn = oAuthResult.getSignIn();
            if (signIn != null && signIn.getStatus() == SignIn.Status.COMPLETE) {
                finishWithSuccess();
                return;
            }

            SignUp signUp = oAuthResult.getSignUp();
            if (signUp != null) {
                if (signUp.getStatus() == SignUp.Status.COMPLETE) {
                    finishWithSuccess();
                    return;
                }

                if (signUp.getStatus() == SignUp.Status.MISSING_REQUIREMENTS) {
                    startGoogleCompletion(signUp);
                    return;
                }
            }

            showError(getString(R.string.auth_google_incomplete));
            return;
        }

        showError(getFailureMessage((ClerkResult.Failure<ClerkErrorResponse>) result));
    }

    private void submitSignUp() {
        if (pendingGoogleCompletionSignUp != null) {
            submitGoogleCompletion();
            return;
        }

        String fullName = safeText(signUpNameInput.getText());
        String username = safeText(signUpUsernameInput.getText()).toLowerCase(Locale.US);
        String email = safeText(signUpEmailInput.getText());
        String password = safeText(signUpPasswordInput.getText());

        if (fullName.isEmpty()) {
            showError(getString(R.string.auth_name_required));
            return;
        }
        if (username.isEmpty()) {
            showError(getString(R.string.auth_username_required));
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.auth_email_invalid));
            return;
        }
        if (password.isEmpty()) {
            showError(getString(R.string.auth_password_required));
            return;
        }

        String[] names = splitName(fullName);
        String clerkUsername = isClerkUsernameEnabled() ? username : null;

        setLoading(true);
        executor.execute(() -> {
            try {
                ClerkResult<SignUp, ClerkErrorResponse> result = ClerkSuspendUtils.await(
                        continuation -> SignUp.Companion.create(
                                new SignUp.CreateParams.Standard(
                                        email,
                                        password,
                                        names[0],
                                        names[1],
                                        clerkUsername,
                                        null
                                ),
                                continuation
                        )
                );
                runOnUiThread(() -> handleSignUpResult(result));
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(resolveThrowableMessage(exception));
                });
            }
        });
    }

    private void handleSignUpResult(ClerkResult<SignUp, ClerkErrorResponse> result) {
        if (result instanceof ClerkResult.Success) {
            SignUp signUp = ((ClerkResult.Success<SignUp>) result).getValue();
            pendingVerificationSignUp = signUp;
            shouldPersistSignUpProfile = true;

            if (signUp != null && signUp.getStatus() == SignUp.Status.COMPLETE) {
                setLoading(false);
                finishWithSuccess();
                return;
            }

            requestVerificationCode();
            return;
        }

        setLoading(false);
        showError(getFailureMessage((ClerkResult.Failure<ClerkErrorResponse>) result));
    }

    private void requestVerificationCode() {
        if (pendingVerificationSignUp == null) {
            setLoading(false);
            showError(getString(R.string.auth_generic_error));
            return;
        }

        executor.execute(() -> {
            try {
                ClerkResult<SignUp, ClerkErrorResponse> result = ClerkSuspendUtils.await(
                        continuation -> SignUpKt.prepareVerification(
                                pendingVerificationSignUp,
                                new SignUp.PrepareVerificationParams.Strategy.EmailCode(),
                                continuation
                        )
                );
                runOnUiThread(() -> handlePrepareVerificationResult(result));
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(resolveThrowableMessage(exception));
                });
            }
        });
    }

    private void handlePrepareVerificationResult(ClerkResult<SignUp, ClerkErrorResponse> result) {
        setLoading(false);

        if (result instanceof ClerkResult.Success) {
            pendingVerificationSignUp = ((ClerkResult.Success<SignUp>) result).getValue();
            switchMode(MODE_VERIFY);
            showStatus(getString(R.string.auth_verification_sent));
            return;
        }

        showError(getFailureMessage((ClerkResult.Failure<ClerkErrorResponse>) result));
    }

    private void submitVerification() {
        String code = safeText(verificationCodeInput.getText());
        if (pendingVerificationSignUp == null) {
            showError(getString(R.string.auth_verification_expired));
            switchMode(MODE_SIGN_UP);
            return;
        }
        if (code.isEmpty()) {
            showError(getString(R.string.auth_verification_required));
            return;
        }

        setLoading(true);
        executor.execute(() -> {
            try {
                ClerkResult<SignUp, ClerkErrorResponse> result = ClerkSuspendUtils.await(
                        continuation -> SignUpKt.attemptVerification(
                                pendingVerificationSignUp,
                                new SignUp.AttemptVerificationParams.EmailCode(code, "email_code"),
                                continuation
                        )
                );
                runOnUiThread(() -> handleVerificationResult(result));
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(resolveThrowableMessage(exception));
                });
            }
        });
    }

    private void handleVerificationResult(ClerkResult<SignUp, ClerkErrorResponse> result) {
        setLoading(false);

        if (result instanceof ClerkResult.Success) {
            pendingVerificationSignUp = ((ClerkResult.Success<SignUp>) result).getValue();
            if (pendingVerificationSignUp != null
                    && pendingVerificationSignUp.getStatus() == SignUp.Status.COMPLETE) {
                finishWithSuccess();
                return;
            }

            showError(getString(R.string.auth_verification_incomplete));
            return;
        }

        showError(getFailureMessage((ClerkResult.Failure<ClerkErrorResponse>) result));
    }

    private void resendVerificationCode() {
        if (pendingVerificationSignUp == null) {
            showError(getString(R.string.auth_verification_expired));
            switchMode(MODE_SIGN_UP);
            return;
        }

        setLoading(true);
        requestVerificationCode();
    }

    private void startGoogleCompletion(SignUp signUp) {
        pendingGoogleCompletionSignUp = signUp;
        pendingVerificationSignUp = null;
        shouldPersistSignUpProfile = true;
        prefillGoogleCompletionInputs(signUp);
        switchMode(MODE_SIGN_UP);
        showStatus(getGoogleCompletionMessage(signUp));
    }

    private void submitGoogleCompletion() {
        if (pendingGoogleCompletionSignUp == null) {
            showError(getString(R.string.auth_google_incomplete));
            return;
        }

        String fullName = safeText(signUpNameInput.getText());
        String username = safeText(signUpUsernameInput.getText()).toLowerCase(Locale.US);
        String email = getPendingGoogleEmail();

        if (fullName.isEmpty()) {
            showError(getString(R.string.auth_name_required));
            return;
        }
        if (username.isEmpty()) {
            showError(getString(R.string.auth_username_required));
            return;
        }
        if (email.isEmpty()) {
            showError(getString(R.string.auth_email_invalid));
            return;
        }

        String[] names = splitName(fullName);
        String clerkUsername = isClerkUsernameEnabled() ? username : null;

        setLoading(true);
        executor.execute(() -> {
            try {
                ClerkResult<SignUp, ClerkErrorResponse> result = ClerkSuspendUtils.await(
                        continuation -> SignUpKt.update(
                                pendingGoogleCompletionSignUp,
                                new SignUp.SignUpUpdateParams.Standard(
                                        email,
                                        null,
                                        names[0],
                                        names[1],
                                        clerkUsername,
                                        null
                                ),
                                continuation
                        )
                );
                runOnUiThread(() -> handleGoogleCompletionResult(result));
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(resolveThrowableMessage(exception));
                });
            }
        });
    }

    private void handleGoogleCompletionResult(ClerkResult<SignUp, ClerkErrorResponse> result) {
        setLoading(false);

        if (result instanceof ClerkResult.Success) {
            SignUp signUp = ((ClerkResult.Success<SignUp>) result).getValue();
            pendingGoogleCompletionSignUp = signUp;

            if (signUp != null && signUp.getStatus() == SignUp.Status.COMPLETE) {
                finishWithSuccess();
                return;
            }

            if (signUp != null && signUp.getStatus() == SignUp.Status.MISSING_REQUIREMENTS) {
                prefillGoogleCompletionInputs(signUp);
                showError(getGoogleCompletionMessage(signUp));
                return;
            }

            showError(getString(R.string.auth_google_incomplete));
            return;
        }

        showError(getFailureMessage((ClerkResult.Failure<ClerkErrorResponse>) result));
    }

    private void prefillGoogleCompletionInputs(SignUp signUp) {
        signUpNameInput.setText(joinName(signUp.getFirstName(), signUp.getLastName()));
        signUpEmailInput.setText(safeText(signUp.getEmailAddress()));
        signUpUsernameInput.setText(safeText(signUp.getUsername()));
        signUpPasswordInput.setText(null);
    }

    private String getGoogleCompletionMessage(SignUp signUp) {
        if (signUp != null && signUp.getMissingFields() != null
                && signUp.getMissingFields().contains("username")) {
            return getString(R.string.auth_google_missing_username);
        }
        return getString(R.string.auth_google_missing_fields);
    }

    private void clearPendingGoogleCompletion() {
        pendingGoogleCompletionSignUp = null;
        updateSignUpFormState();
    }

    private void clearSignUpProfileDraft() {
        shouldPersistSignUpProfile = false;
    }

    private void updateSignUpFormState() {
        boolean isGoogleCompletion = pendingGoogleCompletionSignUp != null;

        signUpButton.setText(isGoogleCompletion
                ? R.string.auth_google_complete_cta
                : R.string.auth_sign_up_cta);
        signUpPasswordLayout.setVisibility(isGoogleCompletion ? View.GONE : View.VISIBLE);
        signUpEmailInput.setEnabled(!isGoogleCompletion);
        signUpEmailLayout.setEnabled(true);

        if (!isGoogleCompletion) {
            signUpPasswordLayout.setVisibility(View.VISIBLE);
        }
    }

    private String getPendingGoogleEmail() {
        if (pendingGoogleCompletionSignUp != null
                && pendingGoogleCompletionSignUp.getEmailAddress() != null) {
            return pendingGoogleCompletionSignUp.getEmailAddress().trim();
        }
        return safeText(signUpEmailInput.getText());
    }

    private String joinName(String firstName, String lastName) {
        String first = safeText(firstName);
        String last = safeText(lastName);
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    private void finishWithSuccess() {
        pendingVerificationSignUp = null;
        pendingGoogleCompletionSignUp = null;
        try {
            Clerk.INSTANCE.updateSessionAndUserState$source_release();
            ClerkSessionBridge.syncSessionBlocking(getApplicationContext());
        } catch (RuntimeException ignored) {
            // Stored token refresh is best-effort here.
        }

        if (!ClerkSessionBridge.isSignedIn(this) && !new ClerkAuthHelper(this).isLoggedIn()) {
            showError(getString(R.string.auth_session_sync_failed));
            return;
        }

        ClerkAuthHelper authHelper = new ClerkAuthHelper(this);
        authHelper.setGuestModeEnabled(false);
        new LocalProfileStore(this).clearGuestProfile();
        saveClerkProfileIfAvailable(authHelper);
        persistSignUpProfileDraftIfNeeded(authHelper);
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void finishAsGuest() {
        pendingVerificationSignUp = null;
        pendingGoogleCompletionSignUp = null;

        ClerkAuthHelper authHelper = new ClerkAuthHelper(this);
        authHelper.clearToken();
        authHelper.setGuestModeEnabled(true);
        authHelper.setOnboardingCompleted(true);
        new LocalProfileStore(this).createAndSaveRandomGuestProfile();
        clearSignUpProfileDraft();

        setResult(RESULT_CONTINUE_AS_GUEST);
        finish();
    }

    private void persistSignUpProfileDraftIfNeeded(ClerkAuthHelper authHelper) {
        if (!shouldPersistSignUpProfile) {
            return;
        }

        String username = safeText(signUpUsernameInput.getText()).toLowerCase(Locale.US);
        String fullName = safeText(signUpNameInput.getText());
        if (username.isEmpty() && fullName.isEmpty()) {
            shouldPersistSignUpProfile = false;
            return;
        }

        User profile = new User();
        profile.setId(authHelper.getUserId());
        profile.setUsername(fullName.isEmpty() ? username : fullName);
        profile.setHandle(username.isEmpty() ? "" : "@" + username);
        profile.setEmail(safeText(signUpEmailInput.getText()));
        profile.setBio(safeText(signUpBioInput.getText()));
        profile.setLocation(safeText(signUpLocationInput.getText()));
        profile.setJoinedDate(buildJoinedDate());
        profile.setBirthday(safeText(signUpBirthdayInput.getText()));
        profile.setXp(0);
        profile.setVerified(true);

        new LocalProfileStore(this).saveProfile(profile);
        shouldPersistSignUpProfile = false;
    }

    private String buildJoinedDate() {
        return "Joined " + new SimpleDateFormat("MMM yyyy", Locale.US).format(new Date());
    }

    private boolean isClerkUsernameEnabled() {
        return Clerk.INSTANCE.getUsernameIsEnabled();
    }

    private void saveClerkProfileIfAvailable(ClerkAuthHelper authHelper) {
        com.clerk.user.User clerkUser = Clerk.INSTANCE.getUser() != null
                ? Clerk.INSTANCE.getUser().getValue()
                : null;
        if (clerkUser == null) {
            return;
        }

        LocalProfileStore profileStore = new LocalProfileStore(this);
        User savedProfile = profileStore.getSavedProfile();
        String currentUserId = authHelper.getUserId();
        if (savedProfile != null
                && currentUserId != null
                && savedProfile.getId() != null
                && !currentUserId.equals(savedProfile.getId())) {
            savedProfile = null;
        }

        User profile = savedProfile == null ? new User() : new User(savedProfile);
        profile.setId(currentUserId);
        profile.setVerified(true);

        String fullName = joinName(clerkUser.getFirstName(), clerkUser.getLastName()).trim();
        String fallbackName = !fullName.isEmpty()
                ? fullName
                : safeText(clerkUser.getUsername());
        if (!fallbackName.isEmpty() && (!hasText(profile.getUsername()) || "Coder".equalsIgnoreCase(profile.getUsername()))) {
            profile.setUsername(fallbackName);
        }

        if (!hasText(profile.getHandle()) && hasText(clerkUser.getUsername())) {
            profile.setHandle("@" + clerkUser.getUsername().trim());
        }

        String email = resolvePrimaryEmail(clerkUser);
        if (hasText(email) && !hasText(profile.getEmail())) {
            profile.setEmail(email);
        }

        profileStore.saveProfile(profile);
    }

    private String resolvePrimaryEmail(com.clerk.user.User clerkUser) {
        if (clerkUser == null || clerkUser.getEmailAddresses() == null) {
            return "";
        }

        String primaryEmailId = safeText(clerkUser.getPrimaryEmailAddressId());
        for (EmailAddress emailAddress : clerkUser.getEmailAddresses()) {
            if (emailAddress == null) {
                continue;
            }
            if (!primaryEmailId.isEmpty() && primaryEmailId.equals(emailAddress.getId())) {
                return safeText(emailAddress.getEmailAddress());
            }
        }

        for (EmailAddress emailAddress : clerkUser.getEmailAddresses()) {
            if (emailAddress != null && hasText(emailAddress.getEmailAddress())) {
                return safeText(emailAddress.getEmailAddress());
            }
        }

        return "";
    }

    private String getFailureMessage(ClerkResult.Failure<ClerkErrorResponse> failure) {
        ClerkErrorResponse error = failure.getError();
        if (error != null && error.getErrors() != null && !error.getErrors().isEmpty()) {
            com.clerk.network.model.error.Error firstError = error.getErrors().get(0);
            if (firstError.getLongMessage() != null && !firstError.getLongMessage().trim().isEmpty()) {
                return firstError.getLongMessage();
            }
            if (firstError.getMessage() != null && !firstError.getMessage().trim().isEmpty()) {
                return firstError.getMessage();
            }
        }

        if (failure.getThrowable() != null && failure.getThrowable().getMessage() != null) {
            return failure.getThrowable().getMessage();
        }

        return getString(R.string.auth_generic_error);
    }

    private String resolveThrowableMessage(RuntimeException exception) {
        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
        return cause.getMessage() != null && !cause.getMessage().trim().isEmpty()
                ? cause.getMessage()
                : getString(R.string.auth_generic_error);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        googleButton.setEnabled(!isLoading);
        continueGuestButton.setEnabled(!isLoading);
        signInButton.setEnabled(!isLoading);
        signUpButton.setEnabled(!isLoading);
        verifyButton.setEnabled(!isLoading);
        resendCodeButton.setEnabled(!isLoading);
        signInTabButton.setEnabled(!isLoading);
        signUpTabButton.setEnabled(!isLoading);
    }

    private void clearStatus() {
        statusText.setVisibility(View.GONE);
        statusText.setText("");
        statusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    private void showStatus(String message) {
        statusText.setVisibility(View.VISIBLE);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
        statusText.setText(message);
    }

    private void showError(String message) {
        statusText.setVisibility(View.VISIBLE);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.difficulty_hard));
        statusText.setText(message);
    }

    private String safeText(@Nullable CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String[] splitName(String fullName) {
        String normalized = fullName.trim().replaceAll("\\s+", " ");
        int firstSpace = normalized.indexOf(' ');
        if (firstSpace < 0) {
            return new String[]{normalized, ""};
        }
        return new String[]{
                normalized.substring(0, firstSpace),
                normalized.substring(firstSpace + 1).trim()
        };
    }

    private <T> void executeSuspendCall(
            ClerkSuspendUtils.SuspendCall<ClerkResult<T, ClerkErrorResponse>> call,
            SuspendResultHandler<T> handler
    ) {
        try {
            Object immediate = call.invoke(new Continuation<ClerkResult<T, ClerkErrorResponse>>() {
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(Object result) {
                    handleSuspendCallResult(result, handler);
                }
            });

            if (immediate != IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
                handleSuspendCallResult(immediate, handler);
            }
        } catch (Throwable throwable) {
            setLoading(false);
            showError(resolveThrowableMessage(new RuntimeException(throwable)));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void handleSuspendCallResult(Object result, SuspendResultHandler<T> handler) {
        runOnUiThread(() -> {
            try {
                ResultKt.throwOnFailure(result);
                handler.onResult((ClerkResult<T, ClerkErrorResponse>) result);
            } catch (Throwable throwable) {
                setLoading(false);
                showError(resolveThrowableMessage(new RuntimeException(throwable)));
            }
        });
    }

    private interface SuspendResultHandler<T> {
        void onResult(ClerkResult<T, ClerkErrorResponse> result);
    }
}
