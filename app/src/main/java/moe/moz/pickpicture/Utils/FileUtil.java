/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package moe.moz.pickpicture.Utils;

import android.content.Context;

import java.io.File;

//文件的操作工具类
public class FileUtil {

    //CameraActivity使用的
    public static File getSaveFileForCameraActivity(Context context) {
        File file = new File(context.getFilesDir(), "pic.jpg");
        return file;
    }

    //MainActivity使用的
    public static File getSaveFileForMainActivity(Context context) {
        File file = new File(context.getFilesDir(), "test_pic.jpg");
        return file;
    }
}
