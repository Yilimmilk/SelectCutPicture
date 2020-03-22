package moe.moz.pickpicture;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import hei.permission.EasyPermissions;
import hei.permission.PermissionActivity;
import moe.moz.pickpicture.Utils.FileUtil;

/**
 * @author Yili(yili)
 * @description
 * @package moe.moz.pickpicture
 * @date 2020-03-21
 */
public class CameraActivity extends PermissionActivity {
    private static final int REQUEST_CODE_CAMERA = 1;
    private Uri outputMediaUri;
    //false:send uri  true:copy file
    private static String TAG = "CameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions();

        Intent contentIntent = getIntent();
        String contentAction = contentIntent.getAction();
        this.outputMediaUri = contentIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        Log.d(TAG,"打印outputMediaUri"+outputMediaUri);
        if (contentAction != null) {
            Intent cutIntent = new Intent(CameraActivity.this, com.baidu.ocr.ui.camera.CameraActivity.class);
            String cameraPath = FileUtil.getSaveFileForCameraActivity(getApplication()).getAbsolutePath();
            Log.d(TAG,"将临时文件存储至:"+cameraPath);
            cutIntent.putExtra(com.baidu.ocr.ui.camera.CameraActivity.KEY_OUTPUT_FILE_PATH,
                    cameraPath);
            cutIntent.putExtra(com.baidu.ocr.ui.camera.CameraActivity.KEY_CONTENT_TYPE,
                    com.baidu.ocr.ui.camera.CameraActivity.CONTENT_TYPE_GENERAL);
            startActivityForResult(cutIntent, REQUEST_CODE_CAMERA);
        }
    }

    //接受返回数据
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,requestCode+"");
        Log.d(TAG,resultCode+"");
        //Log.d(TAG,data.getData()+"");
        if (requestCode == EasyPermissions.SETTINGS_REQ_CODE) {
            //设置返回
        }
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAMERA && data != null) {
                //TODO:主要部分，留着研究
                //*****************修改了com.baidu.ocr.ui.camera.CameraActivity的303行*************
                if (data.getData() != null) {
                    Log.d(TAG,"使用saveMediaToExtraFile");
                    saveMediaToExtraFile(data);
                } else {
                    Toast.makeText(this, "你没有选择文件", Toast.LENGTH_SHORT).show();
                    onBackPressed();
                }
            } else {
                onBackPressed();
            }
        } else {
            onBackPressed();
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

    //传输文件回调
    private void saveMediaToExtraFile(final Intent resultIntent) {
        Log.d(TAG,"当前是saveMediaToExtraFile，打印resultIntent.getData:"+resultIntent.getData());
        Log.d(TAG,"当前是saveMediaToExtraFile，打印outputMediaUri:"+outputMediaUri);
        if (resultIntent.getData() != null ) {
            new TransferMediaAsync(getContentResolver(), isSuccess -> {
                if (isSuccess) {
                    exitWithResult(resultIntent);
                    Toast.makeText(CameraActivity.this, "文件传输成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CameraActivity.this, "文件传输错误", Toast.LENGTH_SHORT).show();
                    onBackPressed();
                }

            }).execute(resultIntent.getData(), outputMediaUri);
        }
    }

    //最后的传值
    private void exitWithResult(Intent resultIntent) {
        Log.d(TAG,"执行exitWithResult");
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    //按下返回键之后
    @Override
    public void onBackPressed() {
        Log.d(TAG,"执行onBackPressed");
        setResult(RESULT_CANCELED);
        finish();
    }
}
