package moe.moz.pickpicture.CustomView;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * @author Yili(yili)
 * @description
 * @package moe.moz.pickpicture
 * @date 2020-03-22
 */
public class MyWebView extends WebView {

    private MyWebChromeClient webChromeClient;

    public MyWebView(Context context) {
        super(context);
        initWebView();
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWebView();
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWebView();
    }

    private void initWebView() {
        webChromeClient = new MyWebChromeClient();
        setWebChromeClient(webChromeClient);

        WebSettings webviewSettings = getSettings();
        // 不支持缩放
        webviewSettings.setSupportZoom(false);
        // 自适应屏幕大小
        webviewSettings.setUseWideViewPort(true);
        webviewSettings.setLoadWithOverviewMode(true);
        String cacheDirPath = getContext().getFilesDir().getAbsolutePath() + "cache/";
        webviewSettings.setAppCachePath(cacheDirPath);
        webviewSettings.setAppCacheEnabled(true);
        webviewSettings.setDomStorageEnabled(true);
        webviewSettings.setAllowFileAccess(true);
        webviewSettings.setAppCacheMaxSize(1024 * 1024 * 8);
        webviewSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
    }

    public void setOpenFileChooserCallBack(MyWebChromeClient.OpenFileChooserCallBack callBack) {
        webChromeClient.setOpenFileChooserCallBack(callBack);
    }
}