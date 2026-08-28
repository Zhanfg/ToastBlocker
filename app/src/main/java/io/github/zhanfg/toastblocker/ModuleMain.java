package io.github.zhanfg.toastblocker;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedModule;

public final class ModuleMain extends XposedModule {
    private static final String TAG = "ToastBlocker";
    private static final String PREFS = "rules";
    private static final String KEY_PACKAGES = "packages";
    private static final String KEY_KEYWORDS = "keywords";

    @Override
    public void onPackageReady(PackageReadyParam param) {
        installAppHook(param.getPackageName());
        if ("com.android.systemui".equals(param.getPackageName())) {
            installSystemUiHook(param.getClassLoader());
        }
    }

    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        installSystemServerHook(param.getClassLoader());
    }

    private void installAppHook(String packageName) {
        try {
            Method show = Toast.class.getDeclaredMethod("show");
            hook(show).intercept(chain -> {
                try {
                    CharSequence text = toastText((Toast) chain.getThisObject());
                    if (shouldBlock(packageName, text)) return null;
                } catch (Throwable error) {
                    log(Log.WARN, TAG, "Toast inspection failed; allowing it", error);
                }
                return chain.proceed();
            });
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Unable to hook Toast.show in " + packageName, error);
        }
    }

    private void installSystemUiHook(ClassLoader classLoader) {
        hookMatchingMethods(classLoader, "com.android.systemui.toast.ToastUI", "showToast");
    }

    private void installSystemServerHook(ClassLoader classLoader) {
        hookMatchingMethods(classLoader, "com.android.server.notification.NotificationManagerService", "enqueueTextToast");
    }

    private void hookMatchingMethods(ClassLoader classLoader, String className, String methodName) {
        try {
            Class<?> type = Class.forName(className, false, classLoader);
            int installed = 0;
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) continue;
                hook(method).intercept(chain -> {
                    try {
                        Object[] toast = toastArgs(chain.getArgs());
                        if (shouldBlock((String) toast[0], (CharSequence) toast[1])) return null;
                    } catch (Throwable error) {
                        log(Log.WARN, TAG, methodName + " inspection failed; allowing it", error);
                    }
                    return chain.proceed();
                });
                installed++;
            }
            if (installed == 0) log(Log.WARN, TAG, "No compatible " + className + "." + methodName + " method");
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Unable to hook " + className + "." + methodName, error);
        }
    }

    private boolean shouldBlock(String packageName, CharSequence text) {
        SharedPreferences prefs = getRemotePreferences(PREFS);
        return RuleMatcher.shouldBlock(
                packageName,
                text,
                prefs.getString(KEY_PACKAGES, ""),
                prefs.getString(KEY_KEYWORDS, ""));
    }

    private static CharSequence toastText(Toast toast) throws ReflectiveOperationException {
        try {
            Field textField = Toast.class.getDeclaredField("mText");
            textField.setAccessible(true);
            Object text = textField.get(toast);
            if (text instanceof CharSequence) return (CharSequence) text;
        } catch (NoSuchFieldException ignored) {
            // Android 8-10 keep standard Toast text in its view.
        }

        View view = toast.getView();
        return findText(view);
    }

    private static CharSequence findText(View view) {
        if (view instanceof TextView) return ((TextView) view).getText();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                CharSequence text = findText(group.getChildAt(i));
                if (text != null && text.length() > 0) return text;
            }
        }
        return null;
    }

    private static Object[] toastArgs(List<Object> args) {
        String packageName = null;
        CharSequence text = null;
        for (Object arg : args) {
            if (packageName == null && arg instanceof String) {
                packageName = (String) arg;
            } else if (packageName != null && arg instanceof CharSequence) {
                text = (CharSequence) arg;
                break;
            }
        }
        return new Object[]{packageName, text};
    }
}
