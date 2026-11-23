package io.github.zhanfg.toastblocker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class RuleMatcherTest {
    @Test
    public void blocksOnlySelectedPackageAndMatchingKeyword() {
        String packages = "com.example.one\ncom.example.two";
        String keywords = "广告\n更新成功";

        assertTrue(RuleMatcher.shouldBlock("com.example.one", "这是广告消息", packages, keywords));
        assertFalse(RuleMatcher.shouldBlock("com.other", "这是广告消息", packages, keywords));
        assertFalse(RuleMatcher.shouldBlock("com.example.one", "普通消息", packages, keywords));
        assertFalse(RuleMatcher.shouldBlock("com.example.one", "任意消息", packages, "\n  \n"));
    }

    @Test
    public void clearingEitherListAllowsMatchingToast() {
        assertFalse(RuleMatcher.shouldBlock("com.example.one", "广告", "", "广告"));
        assertFalse(RuleMatcher.shouldBlock("com.example.one", "广告", "  \n", "广告"));
        assertFalse(RuleMatcher.shouldBlock("com.example.one", "广告", null, "广告"));
        assertFalse(RuleMatcher.shouldBlock("com.example.one", "广告", "com.example.one", ""));
        assertFalse(RuleMatcher.shouldBlock("com.example.one", "广告", "com.example.one", null));
        assertFalse(RuleMatcher.shouldBlock("com.example.one", "广告", "", ""));
    }

    @Test
    public void allowsToastWithoutText() {
        assertFalse(RuleMatcher.shouldBlock("com.example.one", null, "com.example.one", "广告"));
    }

    @Test
    public void scopeIncludesSystemTargetsAndSelectedApps() {
        assertEquals(Arrays.asList("system", "com.android.systemui"), RuleMatcher.scope(""));
        assertEquals(Arrays.asList("system", "com.android.systemui", "com.example.one", "com.example.two"),
                RuleMatcher.scope(" com.example.one \ncom.example.one\ncom.example.two"));
    }
}
