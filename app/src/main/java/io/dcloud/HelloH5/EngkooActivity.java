package io.dcloud.HelloH5;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.sina.weibo.sdk.share.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.ThreadPool;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

//小英程序页面
public class EngkooActivity extends BaseActivity {

    public static final int RC_RECORD_AUDIO = 100; //只要不重复就行
    public static final int RC_CAMERA = 101; //只要不重复就行
    public static final int RC_READ_EXTERNAL_STORAGE = 102; //只要不重复就行

    public static final int IMAGE_CHOOSE_RESULT_CODE = 1; //只要不重复就行
    public static final int IMAGE_SHOOT_RESULT_CODE = 2; //只要不重复就行

    private final Activity currentActivity;
    private String currentCameraDestFile;
    private CallBackFunction currentFunction;

    private BridgeWebView webView;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private String title;
    private String englishAssistantScenarioLessonUrl;
    private String accessToken;

    public EngkooActivity() {
        currentActivity = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engkoo);

        //Android 7.0 FileUriExposedException 解决
        if (Build.VERSION.SDK_INT >= 24) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        initStatusBarColor();
        initToolbar();
        initWebView();

        startEngkooWebView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (currentFunction == null) {
            return;
        }

        switch (requestCode) {
            case IMAGE_CHOOSE_RESULT_CODE:
                Uri uri = data.getData();
                String filePath = FileUtil.getFilePathByUri(this, uri);

                if (!TextUtils.isEmpty(filePath)) {
                    callbackWebView(currentFunction, Jpg2String(filePath));
                }
                break;
            case IMAGE_SHOOT_RESULT_CODE:
                if (resultCode == Activity.RESULT_OK && !TextUtils.isEmpty(currentCameraDestFile)) {
                    callbackWebView(currentFunction, Jpg2String(currentCameraDestFile));
                }
                currentCameraDestFile = null;
                break;
        }
    }

    private void startEngkooWebView() {
        Intent intent = getIntent();
        englishAssistantScenarioLessonUrl = intent.getStringExtra("EnglishAssistantScenarioLessonUrl");
        accessToken = intent.getStringExtra("AccessToken");
        title = intent.getStringExtra("Title");

        toolbar.setTitle(title);
        webView.loadUrl(englishAssistantScenarioLessonUrl);
    }

    private void initWebView() {
        progressBar = (ProgressBar) findViewById(R.id.progressbar);//进度条
        webView = (BridgeWebView) findViewById(R.id.webview);

        //声明WebSettings子类
        WebSettings webSettings = webView.getSettings();

        webSettings.setDomStorageEnabled(true); // 开启 DOM storage API 功能
        webSettings.setDatabaseEnabled(true);   //开启 database storage API 功能
        webSettings.setAppCacheEnabled(true);//开启 Application Caches 功能

        webView.setWebChromeClient(webChromeClient);
        webView.setWebViewClient(new BridgeWebViewClient(webView) {
            @Override
            public void onPageFinished(WebView view, String url) {//页面加载完成
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {//页面开始加载
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        webView.registerHandler("voiceRequestFromWeb", new BridgeHandler() {

            @Override
            public void handler(String data, final CallBackFunction function) {

                currentFunction = function;

                switch (data.toUpperCase()) {
                    case "LOG_IN":
                        callbackWebView(function, accessToken);
                        break;
                    case "VOICE_START":
                        startAudioRecording();
                        callbackWebView(function, "ok");
                        break;
                    case "VOICE_STOP":
                        String audioString = AudioRecorder.getInstance().stopRecord();
                        callbackWebView(function, audioString);
                        break;
                }
            }
        });

        webView.registerHandler("NameRequestFromWeb", new BridgeHandler() {

            @Override
            public void handler(String data, final CallBackFunction function) {

                currentFunction = function;

                switch (data.toUpperCase()) {
                    case "IMAGE_CHOOSE":
                        imageChoose();
                        break;
                    case "IMAGE_SHOOT":
                        imageShoot();
                        break;
                }
            }
        });
    }

    @AfterPermissionGranted(RC_READ_EXTERNAL_STORAGE)
    private void imageChoose() {

        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            startActivityForResult(intent, IMAGE_CHOOSE_RESULT_CODE);
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.storage_rationale),
                    RC_READ_EXTERNAL_STORAGE, perms);
        }
    }

    @AfterPermissionGranted(RC_CAMERA)
    private void imageShoot() {

        String[] perms = {Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, perms)) {

            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Date curDate = new Date(System.currentTimeMillis());

            File destFile = new File(
                    Environment.getExternalStorageDirectory(),
                    "xd_" + formatter.format(curDate) + ".jpg");
            currentCameraDestFile = destFile.getAbsolutePath();

            File parentFile = destFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }

            Intent _intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri _uri = Uri.fromFile(destFile);
            _intent.putExtra(MediaStore.EXTRA_OUTPUT, _uri);
            _intent.putExtra("android.intent.extras.CAMERA_FACING", 1);//前置摄像头
            startActivityForResult(_intent, IMAGE_SHOOT_RESULT_CODE);
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.camera_rationale),
                    RC_CAMERA, perms);
        }
    }

    private void callbackWebView(final CallBackFunction function, final String result) {
        ThreadPool.self().addThreadTask(new Runnable() {
            @Override
            public void run() {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        function.onCallBack(result);
                    }
                });
            }
        });
    }

    private void initToolbar() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_toolbar_closed_36);//设置导航栏图标
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.inflateMenu(R.menu.english_toolbar_menu);//设置右上角的填充菜单
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int menuItemId = item.getItemId();
                switch (menuItemId) {
                    case R.id.action_menu_1:
                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                        break;
                    case R.id.action_menu_2:
                        if (webView.canGoForward()) {
                            webView.goForward();
                        }
                        break;
                }
                return true;
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();//返回上个页面
            return true;
        }
        return super.onKeyDown(keyCode, event);//退出整个应用程序
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_RECORD_AUDIO)
    private Boolean startAudioRecording() {

        String[] perms = {android.Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            AudioRecorder.getInstance().startRecord();
            return true;
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.record_audio_rationale),
                    RC_RECORD_AUDIO, perms);

            return false;
        }
    }

    //WebChromeClient主要辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
    private WebChromeClient webChromeClient = new WebChromeClient() {
        //加载进度回调
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
        }
    };

    private static String Jpg2String(String filePath) {
        return android.util.Base64.encodeToString(
                BitmapHelper.getCompressImage(filePath, 50),
                Base64.NO_WRAP);
    }
}
