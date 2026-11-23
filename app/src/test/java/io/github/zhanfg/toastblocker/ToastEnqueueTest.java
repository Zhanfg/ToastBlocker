package io.github.zhanfg.toastblocker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ToastEnqueueTest {
    private static class ToastMethods {
        void enqueueTextToast(String pkg, Object token, CharSequence text, int duration, int display) {}
        boolean enqueueTextToast(String pkg, Object token, CharSequence text, int duration) { return true; }
        void enqueueToast(String pkg, Object token, CharSequence text, Object callback, int duration) {}
        void enqueueToast(String pkg, String metadata, Object callback, int duration) {}
        int enqueueToast(String pkg, CharSequence text) { return 0; }
    }

    private static ToastEnqueue signature(String name, Class<?>... parameters) throws NoSuchMethodException {
        return ToastEnqueue.from(ToastMethods.class.getDeclaredMethod(name, parameters));
    }

    @Test
    public void locatesTextAndChoosesBlockedResult() throws Exception {
        assertEquals(new ToastEnqueue(2, null), signature("enqueueTextToast",
                String.class, Object.class, CharSequence.class, int.class, int.class));
        assertEquals(new ToastEnqueue(2, false), signature("enqueueTextToast",
                String.class, Object.class, CharSequence.class, int.class));
        assertEquals(new ToastEnqueue(2, null), signature("enqueueToast",
                String.class, Object.class, CharSequence.class, Object.class, int.class));
    }

    @Test
    public void skipsCallbackMetadataAndUnsupportedReturns() throws Exception {
        assertNull(signature("enqueueToast", String.class, String.class, Object.class, int.class));
        assertNull(signature("enqueueToast", String.class, CharSequence.class));
    }
}
