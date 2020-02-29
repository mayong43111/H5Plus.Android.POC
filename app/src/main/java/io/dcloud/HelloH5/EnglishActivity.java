package io.dcloud.HelloH5;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;

import java.io.IOException;

import dc.squareup.okhttp.Callback;
import dc.squareup.okhttp.FormEncodingBuilder;
import dc.squareup.okhttp.MediaType;
import dc.squareup.okhttp.OkHttpClient;
import dc.squareup.okhttp.Request;
import dc.squareup.okhttp.RequestBody;
import dc.squareup.okhttp.Response;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class EnglishActivity extends AppCompatActivity {

    private BridgeWebView webView;
    private ProgressBar progressBar;
    private FloatingActionButton goHomeButton;
    private Toolbar toolbar;
    private final Activity currentActivity;
    private final Application currentApplication;

    private String EnglishAssistantScenarioLessonUrl;
    private String AccessToken;

    public static final MediaType HTTP_FORM
            = MediaType.parse("application/x-www-form-urlencoded");
    public static final int RC_RECORD_AUDIO = 100; //只要不重复就行

    OkHttpClient httpClient = new OkHttpClient();

    public EnglishActivity() {
        currentActivity = this;
        currentApplication = this.getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 5.0以上系统状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_english);

        Intent intent = getIntent();
        EnglishAssistantScenarioLessonUrl = intent.getStringExtra("EnglishAssistantScenarioLessonUrl"); // 没有输入值默认为0
        AccessToken = intent.getStringExtra("AccessToken"); // 没有输入值默认为0

        progressBar = (ProgressBar) findViewById(R.id.progressbar);//进度条
        webView = (BridgeWebView) findViewById(R.id.webview);
        goHomeButton = (FloatingActionButton) findViewById(R.id.goback_continue);

        goHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentActivity.finish();
            }
        });

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

                switch (data.toUpperCase()) {
                    case "LOG_IN":
                        //function.onCallBack(loadUserAccessToken());
                        function.onCallBack(AccessToken);
                        break;
                    case "VOICE_START":
                        startAudioRecording();
                        function.onCallBack("ok");
                        break;
                    case "VOICE_STOP":
                        String audioString = AudioRecorder.getInstance().stopRecord();
                        function.onCallBack(audioString);
                        break;
                }
            }
        });

        webView.loadUrl(EnglishAssistantScenarioLessonUrl);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
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

//    private String loadUserAccessToken() throws InterruptedException {
//
//        RequestBody formBody = new FormEncodingBuilder()
//                .add("grant_type", EnglishAssistantGrantType)
//                .add("id", EnglishAssistantID) //TODO: 学员ID
//                .add("secret", EnglishAssistantSecret)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(EnglishAssistantOAuthUrl)
//                .post(formBody)
//                .build();
//
//        final Object object = new Object();
//        synchronized (object) {
//            final String[] access_token = new String[1];
//
//            httpClient.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(Request request, IOException e) {
//                    synchronized (object) {
//                        object.notify();
//                    }
//                }
//
//                @Override
//                public void onResponse(Response response) throws IOException {
//                    if (response.isSuccessful()) {
//                        access_token[0] = response.body().string();
//                    }
//                    synchronized (object) {
//                        object.notify();
//                    }
//                }
//            });
//            object.wait();
//            return access_token[0];
//        }
//    }
}
