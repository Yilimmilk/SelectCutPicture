package moe.moz.pickpicture;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import hei.permission.PermissionActivity;

/**
 * @author Yili(yili)
 * @description
 * @package moe.moz.pickpicture
 * @date 2020-03-20
 */
public class MainActivity extends PermissionActivity implements OnClickListener {

    private String webUrl = "file:////android_asset/index.html";
    private String webOpenSourceUrl = "";
    private String webMyPageUrl = "https://moz.moe";
    private WebView wvNote;
    private Button btTest,btCut;
    private ProgressBar pgbLoad;

    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        requestPermissions();
        initview();
        webViewAbout();
    }

    //初始化控件
    private void initview(){
        wvNote = findViewById(R.id.wv_note);
        btTest = findViewById(R.id.bt_test);
        btCut = findViewById(R.id.bt_cut);
        pgbLoad = findViewById(R.id.pgb_webload);

        btTest.setOnClickListener(this);
        btCut.setOnClickListener(this);
    }

    //webview相关代码
    private void webViewAbout(){
        //如果不设置WebViewClient，请求会跳转系统浏览器
        wvNote.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        //设置WebView进度条
        wvNote.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                //显示进度条
                pgbLoad.setProgress(newProgress);
                if (newProgress == 100) {
                    //加载完毕隐藏进度条
                    pgbLoad.setVisibility(View.GONE);
                } else {
                    pgbLoad.setVisibility(View.VISIBLE);
                    pgbLoad.setProgress(newProgress);
                }
                super.onProgressChanged(view, newProgress);
            }
        });
        WebSettings settings = wvNote.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true); //设置可以访问文件
        settings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        settings.setLoadsImagesAutomatically(true); //支持自动加载图片
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        wvNote.loadUrl(webUrl);
    }

    //请求相关权限
    private void requestPermissions() {
        checkPermission(new CheckPermListener() {
                            @Override
                            public void superPermission() {
                                //TODO : 需要权限去完成的功能

                            }
                        },R.string.note,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_test:
                Intent intent=new Intent();
                // 指定开启系统相机的Action
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                startActivityForResult(intent, 0);
                break;
            case R.id.bt_cut:
                //TODO 开始裁剪按钮
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //右上角菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //清理缓存
        if (id == R.id.action_clear) {
            wvNote.clearCache(true);
        }
        //开源相关
        if (id == R.id.action_opensource) {
            wvNote.loadUrl("");
        }
        //关于
        if (id == R.id.action_about) {
            //将dialog_change_layout渲染成view对象
            View aboutView = View.inflate(MainActivity.this, R.layout.dialog_about, null);
            //记一个坑，一定要用对应布局的view去findviewbyid
            TextView tvDiaNote = aboutView.findViewById(R.id.tv_dia_note);
            //构建弹窗并检查数据合法性
            AlertDialog alertDialogSearch = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("关于")
                    .setView(aboutView)
                    .setCancelable(true)
                    .setPositiveButton("确定", null)
                    .setNeutralButton("我的主页", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Uri uri = Uri.parse(webMyPageUrl);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    })
                    .create();
            alertDialogSearch.show();
        }
        return super.onOptionsItemSelected(item);
    }
}
