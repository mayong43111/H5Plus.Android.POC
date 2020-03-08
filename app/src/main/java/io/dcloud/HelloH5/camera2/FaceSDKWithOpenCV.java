package io.dcloud.HelloH5.camera2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.dcloud.HelloH5.R;

public class FaceSDKWithOpenCV {

    private static final String TAG = "FaceSDKWithOpenCV";
    private CascadeClassifier classifier;

    // 手动装载openCV库文件，以保证手机无需安装OpenCV Manager
    static {
        System.loadLibrary("opencv_java3");
    }

    // 初始化人脸级联分类器，必须先初始化
    public FaceSDKWithOpenCV(Activity activity) {
        try {
            InputStream is = activity.getResources()
                    .openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = activity.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Rect> detect(Mat src, int width, int height) {

        org.opencv.core.Size minSize = new org.opencv.core.Size(Math.round(height * 0.2), Math.round(height * 0.2));
        org.opencv.core.Size maxSize = new org.opencv.core.Size();

        MatOfRect faces = new MatOfRect();

        if (classifier != null)
            classifier.detectMultiScale(src, faces, 1.1, 2, 2, minSize, maxSize);

        org.opencv.core.Rect[] facesArray = faces.toArray();
        Log.e(TAG, "找到脸部数量:" + facesArray.length);

        if (facesArray.length == 0) {
            return null;
        }

        List<Rect> rects = new ArrayList<>();

        Rect r = null;
        for (org.opencv.core.Rect faceRect : facesArray) {
            r = new Rect();
            r.left = (int) faceRect.x;
            r.right = (int) (faceRect.x + faceRect.width);
            r.top = (int) (faceRect.y);
            r.bottom = (int) (faceRect.y + faceRect.height);
            Log.d(TAG, r.toString());
            rects.add(r);
        }

        return rects;
    }
}
