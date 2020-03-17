package io.dcloud.HelloH5;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import io.dcloud.HelloH5.camera.CameraView;
import io.dcloud.HelloH5.camera.FaceSDK;
import io.dcloud.HelloH5.camera2.FaceSDKWithOpenCV;

public class LoginDetectFaceActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private FaceSDKWithOpenCV mFaceSDK;
    private CameraView mCameraView;
    private Handler mBackgroundHandler;
    long lastModirTime;
    private CameraView.Callback mCallback = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
        }

        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera) {

            if (lastModirTime == 0) {
                lastModirTime = System.currentTimeMillis();
                return;
            }

            if ((System.currentTimeMillis() - lastModirTime) <= 200 || data == null || data.length == 0) {
                return;
            }

            Log.i(TAG, "onPreviewFrame " + (data == null ? null : data.length));
            getBackgroundHandler().post(new FaceThread(data, camera, mFaceSDK, new FaceSDKCallBackFunction() {
                @Override
                public void onCallBack(Bitmap bitmap, List<Rect> rects) {

                    if (0 == rects.size()) {
                        return;
                    }

                    Intent data = new Intent();
                    data.putExtra("return_data", getFaceImgBase64(bitmap, rects.get(0)));
                    setResult(1, data);
                    finish();
                }
            }));
            lastModirTime = System.currentTimeMillis();
        }
    };

    private String getFaceImgBase64(Bitmap bitmap, Rect rect) {

        Bitmap newBitmap = BitmapHelper.DrawRectangles(bitmap, rect);
        byte[] bitmapBytes = BitmapHelper.readBitmap(newBitmap);

        return android.util.Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_detect_face);

        mCameraView = findViewById(R.id.camera);

        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }

        mFaceSDK = new FaceSDKWithOpenCV(this);

        initToolbar();
        initStatusBarColor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance("获取相机权限失败",
                            new String[]{android.Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            "没有相机权限，app不能为您进行脸部检测")
                    .show(getSupportFragmentManager(), "");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "获取到拍照权限",
                            Toast.LENGTH_SHORT).show();
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    private void initToolbar() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_toolbar_closed_36);//设置导航栏图标
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initStatusBarColor() {
        // 5.0以上系统状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }


    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(String message,
                                                             String[] permissions, int requestCode, String notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putString(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                    if (permissions == null) {
                                        throw new IllegalArgumentException();
                                    }
                                    ActivityCompat.requestPermissions(getActivity(),
                                            permissions, args.getInt(ARG_REQUEST_CODE));
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(),
                                            args.getString(ARG_NOT_GRANTED_MESSAGE),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .create();
        }

    }

    public interface FaceSDKCallBackFunction {

        public void onCallBack(Bitmap bitmap, List<Rect> rects);
    }

    private class FaceThread implements Runnable {

        private byte[] mData;
        private Mat mSrcMat;
        private Mat mDesMat;
        //        private ByteArrayOutputStream mBitmapOutput;
//        private Matrix mMatrix;
        private Camera mCamera;
        private FaceSDKCallBackFunction mCallBackFunction;
        private FaceSDKWithOpenCV mFaceSDK;

        public FaceThread(byte[] data, Camera camera, FaceSDKWithOpenCV faceSDK, FaceSDKCallBackFunction callBackFunction) {
            mData = data;
//            mBitmapOutput = new ByteArrayOutputStream();
//            mMatrix = new Matrix();
//            int mOrienta = mCameraView.getCameraDisplayOrientation();
//            mMatrix.postRotate(mOrienta * -1);
//            mMatrix.postScale(-1, 1);//默认是前置摄像头，直接写死 -1 。
            mCamera = camera;
            mFaceSDK = faceSDK;
            mCallBackFunction = callBackFunction;
        }

        @Override
        public void run() {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                int width = parameters.getPreviewSize().width;
                int height = parameters.getPreviewSize().height;

                mSrcMat = new Mat(height, width, CvType.CV_8UC1);//CV_8UC1---则可以创建----8位无符号的单通道---灰度图片

                mSrcMat.put(0, 0, mData);
                Core.rotate(mSrcMat, mSrcMat, Core.ROTATE_90_COUNTERCLOCKWISE);
                Core.flip(mSrcMat, mSrcMat, 1);

                mDesMat = new Mat(width, height, CvType.CV_8UC1);//旋转了90度
                Imgproc.cvtColor(mSrcMat, mDesMat, Imgproc.COLOR_YUV2GRAY_420);

                List<Rect> rects = mFaceSDK.detect(mDesMat, width, height);

                if (null != rects && rects.size() != 0) {
                    //回调
                    Log.i(TAG, "检测到有" + rects.size() + "人脸");

                    ByteArrayOutputStream bitmapOutput = new ByteArrayOutputStream();

                    try {
                        YuvImage yuv = new YuvImage(mData, parameters.getPreviewFormat(), width, height, null);
                        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, bitmapOutput);

                        byte[] bytes = bitmapOutput.toByteArray();
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        bitmapOutput.reset();

                        // 旋转图片 动作
                        Matrix matrix = new Matrix();
                        matrix.setScale(-1, 1); //水平旋转
                        matrix.postRotate(90);//转正
                        // 创建新的图片
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                        mCallBackFunction.onCallBack(bitmap, rects);
                    } finally {
                        if (bitmapOutput != null) {
                            try {
                                bitmapOutput.close();
                                bitmapOutput = null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mData = null;
            }
        }
//        @Override
//        public void run() {
//            Log.i(TAG, "thread is run");
//            Bitmap bitmap = null;
//            Bitmap roteBitmap = null;
//            try {
//                Camera.Parameters parameters = mCamera.getParameters();
//                int width = parameters.getPreviewSize().width;
//                int height = parameters.getPreviewSize().height;
//
//                YuvImage yuv = new YuvImage(mData, parameters.getPreviewFormat(), width, height, null);
//                mData = null;
//                yuv.compressToJpeg(new Rect(0, 0, width, height), 100, mBitmapOutput);
//
//                byte[] bytes = mBitmapOutput.toByteArray();
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测
//                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//
//                mBitmapOutput.reset();
//                roteBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mMatrix, false);
//                List<Rect> rects = FaceSDK.detectionBitmap(bitmap, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
//
//                if (null == rects || rects.size() == 0) {
//                    Log.i("janecer", "没有检测到人脸哦");
//                } else {
//                    Log.i("janecer", "检测到有" + rects.size() + "人脸");
//                    //回调
//                    mCallBackFunction.onCallBack(bitmap, rects);
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                mMatrix = null;
//                if (bitmap != null) {
//                    bitmap.recycle();
//                }
//                if (roteBitmap != null) {
//                    roteBitmap.recycle();
//                }
//
//                if (mBitmapOutput != null) {
//                    try {
//                        mBitmapOutput.close();
//                        mBitmapOutput = null;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
    }
}
