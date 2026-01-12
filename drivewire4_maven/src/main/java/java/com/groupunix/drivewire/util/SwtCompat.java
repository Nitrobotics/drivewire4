package com.groupunix.drivewire.util;

import java.lang.reflect.Method;

/**
 * Small helper for optional SWT APIs that exist only on some ports/versions.
 *
 * IMPORTANT: This is for *optional* behavior only (cosmetic tweaks, best-effort calls).
 * Do not use this to select which SWT jar to load; that must be handled by the build/runtime classpath.
 */
public final class SwtCompat {
    private SwtCompat() {}

    public static boolean invokeIfPresent(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        if (target == null) return false;
        try {
            Method m = target.getClass().getMethod(methodName, paramTypes);
            m.setAccessible(true);
            m.invoke(target, args);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeIfPresentReturn(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName, paramTypes);
            m.setAccessible(true);
            return (T) m.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean invokeStaticIfPresent(Class<?> clazz, String methodName, Class<?>[] paramTypes, Object... args) {
        if (clazz == null) return false;
        try {
            Method m = clazz.getMethod(methodName, paramTypes);
            m.setAccessible(true);
            m.invoke(null, args);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
