package moe.moz.pickpicture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import hei.permission.PermissionActivity;
import moe.moz.pickpicture.Utils.FileUtil;
import moe.moz.pickpicture.Utils.RomUtil;
import moe.moz.pickpicture.Utils.SqlHelperUtil;

/**
 * @author Yili(yili)
 * @description
 * @package moe.moz.pickpicture
 * @date 2020-03-20
 */
public class MainActivity extends PermissionActivity {

    private static final int SELECT_REQUEST_CODE = 11;
    private static final String webOpenSourceUrl = "https://github.com/Yilimmilk/SelectCutPicture";
    private static final String webMyPageUrl = "https://moz.moe";
    private static final String webHuaweiStettingsUrl = "https://jingyan.baidu.com/article/915fc4143d343b11394b20ef.html";
    private static final String webOppoSettingsUrl = "https://zhidao.baidu.com/question/747241131409415892.html";
    private static final String webVivoSettingsUrl = "http://www.coozhi.com/youxishuma/shouji/59229.html";
    private String BUILD_TIME = BuildConfig.BUILD_TIME;
    private String VERSION_NAME = BuildConfig.VERSION_NAME;
    private String descTitle = "";
    private Toolbar toolbar;
    private ImageView ivDiaShow;
    private Button btDiaSelect;
    private AlertDialog alertDialogTest;
    //拍照图片的Uri
    private Uri imageUri;
    private Bitmap bitmap;
    private Intent dataResult;

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

        ifIsHuaWei();
        ifIsVivo();
        ifIsOppo();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View cutView = View.inflate(MainActivity.this, R.layout.dialog_cut, null);
                ivDiaShow = cutView.findViewById(R.id.iv_dia_show);
                btDiaSelect = cutView.findViewById(R.id.bt_dia_select);
                alertDialogTest = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("手动裁剪")
                        .setView(cutView)
                        .setCancelable(false)
                        .setPositiveButton("存到相册", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (dataResult!=null){
                                    saveBitmap();
                                    dataResult = null;
                                }else {
                                    Snackbar.make(view,"获取数据为空，可能是没有选择图片?",Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();

                btDiaSelect.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 创建文件保存拍照的图片
                        File imageFile = FileUtil.getSaveFileForMainActivity(MainActivity.this);
                        // 获取图片文件的uri对象
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            String authority = getPackageName() + ".provider";
                            imageUri = FileProvider.getUriForFile(MainActivity.this, authority, imageFile);
                        } else {
                            imageUri = Uri.fromFile(imageFile);
                        }
                        // 创建Intent，用于启动手机的照相机拍照
                        Intent intent = new Intent(MainActivity.this,CameraActivity.class);
                        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                        // 指定输出到文件uri中
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        // 启动intent开始拍照
                        Log.d("MainActivity的Uri:", intent.getData() + "");
                        startActivityForResult(intent, SELECT_REQUEST_CODE);
                    }
                });
                alertDialogTest.show();
            }
        });

        //新建线程用于连接数据库
        try {
            //开启一个线程用于读取数据库
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Sql选择语句
    public String SelectDbNote() {
        String sql = "SELECT * FROM `info`";
        String jsonResult = null;
        try {
            // serverlink.ExecuteQuery()，参数1：查询语句，参数2：查询用到的变量，用于本案例不需要参数，所以用空白的new
            // ArrayList<Object>()
            jsonResult = serverlink.ExecuteQuery(sql, new ArrayList<Object>());
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
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
            // 控制台输出，用于监视，与实际使用无关
            try {
                //下边是解析json
                JSONArray jsonArray = new JSONArray(jsonResult);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject object2 = jsonArray.getJSONObject(i);
                    String title = object2.getString("title");
                    if (!TextUtils.isEmpty(title)) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("通知")
                                .setMessage(title)
                                .setCancelable(true)
                                .setPositiveButton("确定", null)
                                .show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // log提示
            Log.d("调试", "连接服务器正常");
        }
    };


    //接收返回数据
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    // 展示拍照后裁剪的图片
                    if (data.getData() != null) {
                        // 根据文件流解析生成Bitmap对象
                        bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()), null, null);
                        dataResult = data;
                        // 展示图片
                        ivDiaShow.setImageBitmap(bitmap);
                        btDiaSelect.setText("重新裁剪");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
                        }, R.string.note,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE);
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
        //开源相关
        if (id == R.id.action_opensource) {
            Uri uri = Uri.parse(webOpenSourceUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
        //关于
        if (id == R.id.action_about) {
            View aboutView = View.inflate(MainActivity.this, R.layout.dialog_about, null);
            TextView tvBuildDate = aboutView.findViewById(R.id.tv_dia_buildtime);
            TextView tvVersion = aboutView.findViewById(R.id.tv_dia_version);
            new AlertDialog.Builder(MainActivity.this)
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
                    }).show();
            tvVersion.setText("版本:" + VERSION_NAME);
            tvBuildDate.setText("编译时间:" + BUILD_TIME);
        }
        return super.onOptionsItemSelected(item);
    }

    //保存图片至本地
    public void saveBitmap() {
        String pickPictureDir=Environment.getExternalStorageDirectory()+"/Pictures/PickPicture/";
        //新建文件夹
        File appDir = new File(Environment.getExternalStorageDirectory()+"/Pictures/", "/PickPicture");
        if (!appDir.exists()) {
            appDir.mkdir();
        }

        Calendar now = new GregorianCalendar();
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        String fileName = simpleDate.format(now.getTime());
        copyFile(FileUtil.getSaveFileForMainActivity(MainActivity.this).getAbsolutePath(),pickPictureDir+fileName+".jpg");
        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(), pickPictureDir+fileName+".jpg", fileName, null);
            Snackbar.make(findViewById(android.R.id.content), "保存图片成功"+pickPictureDir+fileName+".jpg", Snackbar.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Snackbar.make(findViewById(android.R.id.content), "保存失败未知原因", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制单个文件
     *
     * @param oldPath$Name String 原文件路径+文件名 如：data/user/0/com.test/files/abc.txt
     * @param newPath$Name String 复制后路径+文件名 如：data/user/0/com.test/cache/abc.txt
     * @return <code>true</code> if and only if the file was copied;
     *         <code>false</code> otherwise
     */
    public boolean copyFile(String oldPath$Name, String newPath$Name) {
        try {
            File oldFile = new File(oldPath$Name);
            if (!oldFile.exists()) {
                Log.e("--Method--", "copyFile:  oldFile not exist.");
                return false;
            } else if (!oldFile.isFile()) {
                Log.e("--Method--", "copyFile:  oldFile not file.");
                return false;
            } else if (!oldFile.canRead()) {
                Log.e("--Method--", "copyFile:  oldFile cannot read.");
                return false;
            }

            /* 如果不需要打log，可以使用下面的语句
            if (!oldFile.exists() || !oldFile.isFile() || !oldFile.canRead()) {
                return false;
            }
            */

            FileInputStream fileInputStream = new FileInputStream(oldPath$Name);
            FileOutputStream fileOutputStream = new FileOutputStream(newPath$Name);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = fileInputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //判断是否华为手机
    public void ifIsHuaWei() {
        if (RomUtil.isEmui()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示～")
                    .setMessage(getResources().getString(R.string.if_is_huawei))
                    .setCancelable(false)
                    .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent hwIntent = new Intent();
                                hwIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                hwIntent.setClassName("com.android.settings", "com.android.settings.Settings$PreferredListSettingsActivity");
                                startActivity(hwIntent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                try {
                                    Intent intentOther = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                                    startActivity(intentOther);
                                } catch (Exception e1) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, "出错，请按照详细步骤手动打开设置", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    })
                    .setNeutralButton("详细步骤", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Uri uri = Uri.parse(webHuaweiStettingsUrl);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    //判断是否oppo手机
    public void ifIsOppo() {
        if (RomUtil.isOppo()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示～")
                    .setMessage(getResources().getString(R.string.if_is_oppo))
                    .setCancelable(false)
                    .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent hwIntent = new Intent();
                                hwIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                hwIntent.setClassName("com.android.settings", "com.android.settings.Settings$PreferredListSettingsActivity");
                                startActivity(hwIntent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                try {
                                    Intent intentOther = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                                    startActivity(intentOther);
                                } catch (Exception e1) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, "出错，请按照详细步骤手动打开设置", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    })
                    .setNeutralButton("详细步骤", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Uri uri = Uri.parse(webOppoSettingsUrl);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    //判断是否vivo手机
    public void ifIsVivo() {
        if (RomUtil.isVivo()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示～")
                    .setMessage(getResources().getString(R.string.if_is_vivo))
                    .setCancelable(false)
                    .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent hwIntent = new Intent();
                                hwIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                hwIntent.setClassName("com.android.settings", "com.android.settings.Settings$PreferredListSettingsActivity");
                                startActivity(hwIntent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                try {
                                    Intent intentOther = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                                    startActivity(intentOther);
                                } catch (Exception e1) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, "出错，请按照详细步骤手动打开设置", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    })
                    .setNeutralButton("详细步骤", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Uri uri = Uri.parse(webVivoSettingsUrl);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }
}
