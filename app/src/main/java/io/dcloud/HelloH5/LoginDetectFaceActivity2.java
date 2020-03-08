package io.dcloud.HelloH5;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import io.dcloud.HelloH5.camera2.FaceSDKWithOpenCV;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class LoginDetectFaceActivity2 extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2 {

    public static final int RC_CAMERA = 101; //只要不重复就行

    private Mat rgbaMat = null;
    private FaceSDKWithOpenCV faceSDK;
    private CameraBridgeViewBase cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_detect_face2);

        cameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        cameraView.setCvCameraViewListener(this); // 设置相机监听

        faceSDK = new FaceSDKWithOpenCV(this);

        initToolbar();
        initStatusBarColor();

        startCameraView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @AfterPermissionGranted(RC_CAMERA)
    private Boolean startCameraView() {

        String[] perms = {Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, perms)) {
            cameraView.enableView();
            return true;
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.camera_and_location_rationale),
                    RC_CAMERA, perms);

            return false;
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        rgbaMat = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        if (rgbaMat != null)
            rgbaMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        rgbaMat = inputFrame.rgba();
        //Core.rotate(rgbaMat, rgbaMat, Core.ROTATE_90_CLOCKWISE);
        return rgbaMat;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
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
}
