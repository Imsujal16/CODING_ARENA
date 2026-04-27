package com.arena.app.auth;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.arena.app.utils.ClerkAuthHelper;
import com.arena.app.utils.Constants;
import com.clerk.Clerk;
import com.clerk.network.model.error.ClerkErrorResponse;
import com.clerk.network.model.token.TokenResource;
import com.clerk.network.serialization.ClerkResult;
import com.clerk.session.GetTokenOptions;
import com.clerk.session.Session;
import com.clerk.session.SessionKt;
import com.clerk.user.User;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClerkSessionBridge {
    private static final String TAG = "ClerkSessionBridge";
    private static final Object INIT_LOCK = new Object();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile boolean initialized;

    private ClerkSessionBridge() {
    }

    public static void ensureInitialized(Context context) {
        Context initContext = context instanceof android.app.Activity
                ? context
                : context.getApplicationContext();

        if (initialized) {
            updateContextReference(initContext);
            return;
        }

        synchronized (INIT_LOCK) {
            if (initialized) {
                updateContextReference(initContext);
                return;
            }

            Clerk.INSTANCE.initialize(initContext, Constants.CLERK_PUBLISHABLE_KEY);
            updateContextReference(initContext);
            initialized = true;
        }
    }

    public static void handleDeepLink(Uri uri) {
        // Email/password auth does not currently require deep-link handling.
    }

    public static boolean isSignedIn(Context context) {
        Context appContext = context.getApplicationContext();
        if (isGuestMode(appContext)) {
            return false;
        }
        ensureInitialized(appContext);
        return Clerk.INSTANCE.isSignedIn() || new ClerkAuthHelper(appContext).shouldUseAuthenticatedApis();
    }

    public static void syncSessionAsync(Context context) {
        Context appContext = context.getApplicationContext();
        if (isGuestMode(appContext)) {
            return;
        }
        ensureInitialized(appContext);
        EXECUTOR.execute(() -> syncSessionBlocking(appContext));
    }

    public static String syncSessionBlocking(Context context) {
        Context appContext = context.getApplicationContext();
        if (isGuestMode(appContext)) {
            return null;
        }
        ensureInitialized(appContext);
        return fetchAndStoreToken(appContext);
    }

    public static String getFreshTokenBlocking(Context context) {
        Context appContext = context.getApplicationContext();
        if (isGuestMode(appContext)) {
            return null;
        }
        ensureInitialized(appContext);
        return fetchAndStoreToken(appContext);
    }

    public static void signOut(Context context, Runnable onComplete) {
        Context appContext = context.getApplicationContext();
        if (isGuestMode(appContext)) {
            clearStoredSession(appContext);
            postCompletion(onComplete);
            return;
        }
        ensureInitialized(appContext);

        EXECUTOR.execute(() -> {
            try {
                ClerkSuspendUtils.await(continuation -> Clerk.INSTANCE.signOut(continuation));
            } catch (RuntimeException exception) {
                Log.w(TAG, "Clerk sign-out failed, clearing local session anyway.", exception);
            } finally {
                clearStoredSession(appContext);
                postCompletion(onComplete);
            }
        });
    }

    public static void clearStoredSession(Context context) {
        ClerkAuthHelper authHelper = new ClerkAuthHelper(context.getApplicationContext());
        authHelper.clearToken();
        authHelper.setGuestModeEnabled(false);
    }

    private static void updateContextReference(Context context) {
        if (context == null) {
            return;
        }

        Clerk.INSTANCE.setApplicationContext$source_release(new WeakReference<>(context));
    }

    private static void postCompletion(Runnable onComplete) {
        if (onComplete == null) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(onComplete);
    }

    @SuppressWarnings("unchecked")
    private static String fetchAndStoreToken(Context context) {
        ClerkAuthHelper authHelper = new ClerkAuthHelper(context);

        try {
            Clerk.INSTANCE.updateSessionAndUserState$source_release();
            Session session = Clerk.INSTANCE.getSession();
            if (session == null) {
                return null;
            }

            saveUser(session.getUser(), authHelper);

            TokenResource cachedToken = session.getLastActiveToken();
            if (cachedToken != null && hasValue(cachedToken.getJwt())) {
                authHelper.saveToken(cachedToken.getJwt());
                return cachedToken.getJwt();
            }

            ClerkResult<TokenResource, ClerkErrorResponse> result = ClerkSuspendUtils.await(
                    continuation -> SessionKt.fetchToken(session, new GetTokenOptions(), continuation)
            );

            if (result instanceof ClerkResult.Success) {
                TokenResource tokenResource =
                        ((ClerkResult.Success<TokenResource>) result).getValue();
                if (tokenResource != null && hasValue(tokenResource.getJwt())) {
                    authHelper.saveToken(tokenResource.getJwt());
                    return tokenResource.getJwt();
                }
            } else if (result instanceof ClerkResult.Failure) {
                Log.w(TAG, describeFailure((ClerkResult.Failure<ClerkErrorResponse>) result));
            }
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to fetch Clerk token.", exception);
        }

        return null;
    }

    private static void saveUser(User clerkUser, ClerkAuthHelper authHelper) {
        if (clerkUser == null && Clerk.INSTANCE.getUser() != null) {
            clerkUser = Clerk.INSTANCE.getUser().getValue();
        }

        if (clerkUser != null && hasValue(clerkUser.getId())) {
            authHelper.saveUserId(clerkUser.getId());
        }
    }

    private static String describeFailure(ClerkResult.Failure<ClerkErrorResponse> failure) {
        ClerkErrorResponse error = failure.getError();
        if (error != null && error.getErrors() != null && !error.getErrors().isEmpty()) {
            com.clerk.network.model.error.Error firstError = error.getErrors().get(0);
            if (hasValue(firstError.getLongMessage())) {
                return firstError.getLongMessage();
            }
            if (hasValue(firstError.getMessage())) {
                return firstError.getMessage();
            }
        }

        Throwable throwable = failure.getThrowable();
        return throwable != null ? throwable.getMessage() : "Unknown Clerk failure";
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isGuestMode(Context context) {
        return new ClerkAuthHelper(context.getApplicationContext()).isGuestModeEnabled();
    }
}
