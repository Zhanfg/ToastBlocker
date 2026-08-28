package io.github.zhanfg.toastblocker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
