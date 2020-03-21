package moe.moz.pickpicture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Telephony.Mms;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import hei.permission.PermissionActivity;
import moe.moz.pickpicture.CustomView.MyWebChromeClient;
import moe.moz.pickpicture.CustomView.MyWebView;
import moe.moz.pickpicture.Utils.SqlHelperUtil;

/**
 * @author Yili(yili)
 * @description
 * @package moe.moz.pickpicture
 * @date 2020-03-20
 */
public class MainActivity extends PermissionActivity {

    private String webOpenSourceUrl = "https://github.com/Yilimmilk/SelectCutPicture";
    private String webMyPageUrl = "https://moz.moe";
    private String descTitle = "";
    private Toolbar toolbar;
    private SharedPreferences sp;
    private Editor editor;
    private TextView tvDescTitle;
    private WebView webView;
    private ValueCallback<Uri> uploadFile;
    private ValueCallback<Uri[]> uploadFiles;

    private SqlHelperUtil serverlink = new SqlHelperUtil("mc.moz.moe", "app_pickpicture", "app_pickpicture", "333333");

    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("截取图片");
        setSupportActionBar(toolbar);
        requestPermissions();
        tvDescTitle = findViewById(R.id.tv_desc_title);
        View testView = View.inflate(MainActivity.this, R.layout.dialog_webview, null);
        webView = testView.findViewById(R.id.wv_dialog_main);

        sp = getSharedPreferences("data", MODE_PRIVATE);
        editor = sp.edit();

        descTitle = sp.getString("title_database",getResources().getString(R.string.text_desc_title));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog alertDialogSearch = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("试试？")
                        .setView(testView)
                        .setCancelable(true)
                        .setPositiveButton("结束", null)
                        .create();
                initWebView();
                webView.loadUrl("file:///android_asset/index.html");
                alertDialogSearch.show();
            }
        });



        //开启一个线程用于读取数据库，控制文本
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                // 通过Message类来传递结果值，先实例化
                String jsonResult = SelectDbNote();
                Message msg = new Message();
                // 下面分别是增删改查方法
                msg.obj = jsonResult;
                // 执行完以后，把msg传到handler，并且触发handler的响应方法
                handler.sendMessage(msg);
            }
        });
        // 进程开始，这行代码不要忘记
        thread.start();
    }

    //Sql选择语句
    public String SelectDbNote() {
        String sql = "SELECT * FROM `info`";
        String jsonResult;
        try {
            // serverlink.ExecuteQuery()，参数1：查询语句，参数2：查询用到的变量，用于本案例不需要参数，所以用空白的new
            // ArrayList<Object>()
            jsonResult = serverlink.ExecuteQuery(sql, new ArrayList<Object>());
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e.getMessage());
            return null;
        }
        return jsonResult;
    }

    //handler传递数据
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 调用super的方法，传入handler对象接收到的msg对象
            //super.handleMessage(msg);
            String jsonResult = (String) msg.obj;
            System.out.println(jsonResult);
            // 控制台输出，用于监视，与实际使用无关
            try {
                //下边是解析json
                JSONArray jsonArray = new JSONArray(jsonResult);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject object2 = jsonArray.getJSONObject(i);
                    String title = object2.getString("title");

                    editor.putString("title_database",title);
                    editor.commit();
                    if (title != null && !TextUtils.isEmpty(title)) {
                        descTitle = title;
                        tvDescTitle.setText(title);
                    }else {
                        descTitle = getResources().getString(R.string.text_desc_title);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // log提示
            Log.d("调试","连接服务器正常");
        }
    };

    //初始化webview
    private void initWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        webView.setWebChromeClient(new WebChromeClient() {
            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                Log.i("test", "openFileChooser 1");
                MainActivity.this.uploadFile = uploadMsg;
                openFileChooseProcess();
            }

            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsgs) {
                Log.i("test", "openFileChooser 2");
                MainActivity.this.uploadFile = uploadMsgs;
                openFileChooseProcess();
            }

            // For Android  > 4.1.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                Log.i("test", "openFileChooser 3");
                MainActivity.this.uploadFile = uploadMsg;
                openFileChooseProcess();
            }

            // For Android  >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             WebChromeClient.FileChooserParams fileChooserParams) {
                Log.i("test", "openFileChooser 4:" + filePathCallback.toString());
                MainActivity.this.uploadFiles = filePathCallback;
                openFileChooseProcess();
                return true;
            }

        });
    }

    private void openFileChooseProcess() {
//        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
//        i.addCategory(Intent.CATEGORY_OPENABLE);
//        i.setType("*/*");
//        startActivityForResult(Intent.createChooser(i, "上传文件"), 0);
        Intent i = new Intent("android.media.action.IMAGE_CAPTURE");
        i.addCategory("android.intent.category.DEFAULT");
        startActivityForResult(Intent.createChooser(i, "上传文件"),0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                if (null != uploadFile) {
                    Uri result = data == null ? null
                            : data.getData();
                    uploadFile.onReceiveValue(result);
                    uploadFile = null;
                }
                if (null != uploadFiles) {
                    Uri result = data == null ? null
                            : data.getData();
                    uploadFiles.onReceiveValue(new Uri[]{result});
                    uploadFiles = null;
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (null != uploadFile) {
                    uploadFile.onReceiveValue(null);
                    uploadFile = null;
                }
            }
        }
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
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
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
        //开源相关
        if (id == R.id.action_opensource) {
            Uri uri = Uri.parse(webOpenSourceUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
        //关于
        if (id == R.id.action_about) {
            View aboutView = View.inflate(MainActivity.this, R.layout.dialog_about, null);
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
