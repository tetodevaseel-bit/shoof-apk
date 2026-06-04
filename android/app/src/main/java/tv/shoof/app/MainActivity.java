package tv.shoof.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final String LOCAL_URL  = "https://localhost";
    private static final String MINI_HOST  = "shoof-tv.net";

    // ─────────────────────────────────────────────────────────────────────────
    // جسر TelegramWebviewProxy — يُحقن كـ Android interface داخل WebView
    // المستوى الأمني: @JavascriptInterface يمنع استدعاءه إلا من خلال JS
    // ─────────────────────────────────────────────────────────────────────────
    private class TelegramBridge {

        /** Mini App تستدعي هذه الطريقة لإرسال أحداث لـ "Telegram Client" **/
        @JavascriptInterface
        public void postEvent(String eventType, String eventData) {
            if (eventType == null) return;

            switch (eventType) {

                // ─── فتح فاتورة دفع (Telegram Stars) ─────────────────────
                case "web_app_open_invoice": {
                    String url = extractInvoiceUrl(eventData);
                    if (url != null) openInTelegram(url);
                    break;
                }

                // ─── المستخدم يضغط زر الإغلاق ─────────────────────────────
                case "web_app_close":
                    goBackToApp();
                    break;

                // ─── Mini App جاهزة + طلب الـ Viewport ───────────────────
                case "web_app_ready":
                case "web_app_request_viewport":
                    runOnUiThread(() -> injectViewportEvent());
                    break;

                // ─── طلب الثيم ────────────────────────────────────────────
                case "web_app_request_theme":
                    runOnUiThread(() -> injectThemeEvent());
                    break;

                // ─── طلب فتح رابط خارجي ──────────────────────────────────
                case "web_app_open_link":
                case "web_app_open_tg_link": {
                    String link = extractStringValue(eventData, "url");
                    if (link == null) link = extractStringValue(eventData, "path_full");
                    if (link != null) openExternal(link);
                    break;
                }
            }
        }

        /** Mini App تستدعي هذه للتحقق من إصدار الـ API **/
        @JavascriptInterface
        public boolean isVersionAtLeast(String version) {
            return true;
        }

        /** Mini App تستدعي هذه للحصول على بيانات من Clipboard **/
        @JavascriptInterface
        public String readTextFromClipboard() {
            return "";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = getBridge().getWebView();

        // حقن الجسر كـ native interface — متاح في كل الصفحات
        webView.addJavascriptInterface(new TelegramBridge(), "TelegramWebviewProxy");

        // حقن JS إضافي بعد تحميل صفحة Mini App
        webView.setWebViewClient(new com.getcapacitor.BridgeWebViewClient(getBridge()) {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.contains(MINI_HOST)) {
                    injectBridgeHelpers(view);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        WebView webView = getBridge().getWebView();
        String url = webView.getUrl();
        if (url != null && url.contains(MINI_HOST)) {
            goBackToApp();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // مساعدات داخلية
    // ─────────────────────────────────────────────────────────────────────────

    /** حقن JS مساعد يُطلق أحداث البداية لـ telegram-web-app.js **/
    private void injectBridgeHelpers(WebView view) {
        String js =
            "(function() {" +
            // انتظر تحميل Telegram.WebApp
            "  function ready(fn) {" +
            "    if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.receiveEvent) { fn(); }" +
            "    else { setTimeout(function(){ ready(fn); }, 80); }" +
            "  }" +
            "  ready(function() {" +
            "    var twa = window.Telegram.WebApp;" +
            // توسيع التطبيق لملء الشاشة
            "    twa.isExpanded = true;" +
            "    twa.viewportHeight = window.innerHeight;" +
            "    twa.viewportStableHeight = window.innerHeight;" +
            // إرسال الـ Viewport
            "    twa.receiveEvent('viewport_changed', {" +
            "      height: window.innerHeight," +
            "      width: window.innerWidth," +
            "      is_expanded: true," +
            "      is_state_stable: true" +
            "    });" +
            // إرسال الثيم (ألوان شوف TV الداكنة)
            "    twa.receiveEvent('theme_changed', {" +
            "      theme_params: {" +
            "        bg_color: '#080808'," +
            "        text_color: '#ffffff'," +
            "        hint_color: '#777777'," +
            "        link_color: '#e5001a'," +
            "        button_color: '#e5001a'," +
            "        button_text_color: '#ffffff'," +
            "        secondary_bg_color: '#151515'" +
            "      }" +
            "    });" +
            "  });" +
            "})();";

        view.evaluateJavascript(js, null);
    }

    private void injectViewportEvent() {
        WebView view = getBridge().getWebView();
        view.evaluateJavascript(
            "if(window.Telegram&&window.Telegram.WebApp)" +
            " window.Telegram.WebApp.receiveEvent('viewport_changed'," +
            " {height:window.innerHeight,width:window.innerWidth,is_expanded:true,is_state_stable:true});",
            null
        );
    }

    private void injectThemeEvent() {
        WebView view = getBridge().getWebView();
        view.evaluateJavascript(
            "if(window.Telegram&&window.Telegram.WebApp)" +
            " window.Telegram.WebApp.receiveEvent('theme_changed'," +
            " {theme_params:{bg_color:'#080808',text_color:'#ffffff',hint_color:'#777',link_color:'#e5001a',button_color:'#e5001a',button_text_color:'#fff',secondary_bg_color:'#151515'}});",
            null
        );
    }

    private void goBackToApp() {
        runOnUiThread(() ->
            getBridge().getWebView().loadUrl(LOCAL_URL)
        );
    }

    /** فتح رابط الدفع في تطبيق Telegram مباشرة **/
    private void openInTelegram(String url) {
        runOnUiThread(() -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                // حاول تطبيق Telegram الرسمي أولاً
                i.setPackage("org.telegram.messenger");
                try { startActivity(i); return; } catch (Exception ignored) {}
                // أو نسخة Telegram X
                i.setPackage("org.thunderdog.challegram");
                try { startActivity(i); return; } catch (Exception ignored) {}
                // fallback: أي متصفح
                i.setPackage(null);
                startActivity(i);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void openExternal(String url) {
        runOnUiThread(() -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    /** استخراج رابط الفاتورة من JSON بسيط **/
    private String extractInvoiceUrl(String json) {
        if (json == null) return null;
        String url = extractStringValue(json, "url");
        if (url != null) return url;
        String slug = extractStringValue(json, "slug");
        if (slug != null) return "https://t.me/$" + slug;
        return null;
    }

    /** استخراج قيمة نصية من JSON خام بدون مكتبة **/
    private String extractStringValue(String json, String key) {
        if (json == null || key == null) return null;
        try {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx < 0) return null;
            int colon = json.indexOf(":", idx + search.length());
            if (colon < 0) return null;
            int q1 = json.indexOf("\"", colon + 1);
            if (q1 < 0) return null;
            int q2 = json.indexOf("\"", q1 + 1);
            if (q2 < 0) return null;
            return json.substring(q1 + 1, q2);
        } catch (Exception e) { return null; }
    }
}
