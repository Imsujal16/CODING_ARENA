package com.arena.app.auth;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.ResultKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

final class ClerkSuspendUtils {
    private static final long TIMEOUT_SECONDS = 60L;

    private ClerkSuspendUtils() {
    }

    interface SuspendCall<T> {
        Object invoke(Continuation<? super T> continuation);
    }

    @SuppressWarnings("unchecked")
    static <T> T await(SuspendCall<T> call) {
        AtomicReference<T> valueRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Continuation<T> continuation = new Continuation<T>() {
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(Object result) {
                try {
                    ResultKt.throwOnFailure(result);
                    valueRef.set((T) result);
                } catch (Throwable throwable) {
                    errorRef.set(throwable);
                } finally {
                    latch.countDown();
                }
            }
        };

        Object immediateResult;
        try {
            immediateResult = call.invoke((Continuation<? super T>) continuation);
        } catch (Throwable throwable) {
            throw wrap(throwable);
        }

        if (immediateResult != IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
            try {
                ResultKt.throwOnFailure(immediateResult);
                return (T) immediateResult;
            } catch (Throwable throwable) {
                throw wrap(throwable);
            }
        }

        try {
            boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                throw new IllegalStateException("Clerk request timed out.");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Clerk request was interrupted.", interruptedException);
        }

        if (errorRef.get() != null) {
            throw wrap(errorRef.get());
        }

        return valueRef.get();
    }

    private static RuntimeException wrap(Throwable throwable) {
        return throwable instanceof RuntimeException
                ? (RuntimeException) throwable
                : new RuntimeException(throwable);
    }
}
