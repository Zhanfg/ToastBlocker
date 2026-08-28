package io.github.zhanfg.toastblocker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import io.github.libxposed.service.XposedService;

public final class MainActivity extends Activity implements App.ServiceListener {
    private static final String PREFS = "rules";
    private static final String KEY_PACKAGES = "packages";
    private static final String KEY_KEYWORDS = "keywords";
    private static final Pattern PACKAGE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");

    private TextView status;
    private EditText packages;
    private EditText keywords;
    private Button save;
    private XposedService service;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);

        TextView title = text("Toast 屏蔽器", 24);
        content.addView(title);
        status = text("正在连接 LSPosed…", 14);
        status.setTextColor(Color.DKGRAY);
        content.addView(status);

        content.addView(text("目标应用包名（每行一个）", 16));
        packages = editor("例如：com.example.app", 5);
        content.addView(packages);

        content.addView(text("屏蔽关键词（每行一个，区分大小写）", 16));
        keywords = editor("例如：广告\n更新成功", 6);
        content.addView(keywords);

        save = new Button(this);
        save.setText("保存规则并申请作用域");
        save.setEnabled(false);
        save.setOnClickListener(v -> saveRules());
        content.addView(save);

        TextView hint = text("规则保存后会立即供 hook 进程读取。首次使用需在 LSPosed 中批准 android、系统界面和目标应用作用域，然后重启对应进程。", 14);
        content.addView(hint);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    @Override
    protected void onStart() {
        super.onStart();
        App.addListener(this);
    }

    @Override
    protected void onStop() {
        App.removeListener(this);
        super.onStop();
    }

    @Override
    public void onServiceChanged(XposedService newService) {
        runOnUiThread(() -> {
            service = newService;
            save.setEnabled(service != null);
            status.setText(service == null ? "未连接：请启用模块并重新打开" : "已连接 " + service.getFrameworkName() + "，API " + service.getApiVersion());
            if (service != null) loadRules();
        });
    }

    private void loadRules() {
        SharedPreferences prefs = service.getRemotePreferences(PREFS);
        packages.setText(prefs.getString(KEY_PACKAGES, ""));
        keywords.setText(prefs.getString(KEY_KEYWORDS, ""));
    }

    private void saveRules() {
        Set<String> packageSet = RuleMatcher.lines(packages.getText().toString());
        Set<String> keywordSet = RuleMatcher.lines(keywords.getText().toString());
        if (packageSet.isEmpty() || keywordSet.isEmpty()) {
            status.setText("至少填写一个应用包名和一个关键词");
            return;
        }
        for (String packageName : packageSet) {
            if (!PACKAGE_NAME.matcher(packageName).matches()) {
                status.setText("无效包名：" + packageName);
                return;
            }
        }

        String packageText = RuleMatcher.join(packageSet);
        String keywordText = RuleMatcher.join(keywordSet);
        SharedPreferences.Editor editor = service.getRemotePreferences(PREFS).edit();
        if (editor == null || !editor.putString(KEY_PACKAGES, packageText).putString(KEY_KEYWORDS, keywordText).commit()) {
            status.setText("规则保存失败");
            return;
        }
        packages.setText(packageText);
        keywords.setText(keywordText);
        status.setText("规则已保存，等待作用域批准");
        service.requestScope(RuleMatcher.scope(packageText), new XposedService.OnScopeEventListener() {
            @Override
            public void onScopeRequestApproved(List<String> approved) {
                runOnUiThread(() -> status.setText("已批准作用域：" + String.join(", ", approved)));
            }

            @Override
            public void onScopeRequestFailed(String message) {
                runOnUiThread(() -> status.setText("作用域申请失败：" + message));
            }
        });
    }

    private TextView text(String value, int sp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setPadding(0, 12, 0, 12);
        return view;
    }

    private EditText editor(String hint, int lines) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setMinLines(lines);
        view.setGravity(android.view.Gravity.TOP);
        view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return view;
    }
}
