package io.github.zhanfg.toastblocker;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

record ToastEnqueue(int textIndex, Boolean blockedResult) {
    static ToastEnqueue from(Method method) {
        String name = method.getName();
        if (!name.equals("enqueueTextToast") && !name.equals("enqueueToast")) return null;
        if (Modifier.isAbstract(method.getModifiers())) return null;
        Class<?> result = method.getReturnType();
        if (result != void.class && result != boolean.class) return null;

        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length == 0 || parameters[0] != String.class) return null;
        for (int i = 1; i < parameters.length; i++) {
            // The Binder declares toast text as CharSequence. Other String arguments may be metadata.
            if (parameters[i] == CharSequence.class) {
                return new ToastEnqueue(i, result == boolean.class ? Boolean.FALSE : null);
            }
        }
        return null;
    }
}
