package io.dcloud.HelloH5;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.sina.weibo.sdk.share.BaseActivity;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

//小英程序页面
public class EngkooActivity extends BaseActivity {

    public static final int RC_RECORD_AUDIO = 100; //只要不重复就行

    private BridgeWebView webView;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private String title;
    private String englishAssistantScenarioLessonUrl;
    private String accessToken;

    public EngkooActivity(){
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engkoo);

        initStatusBarColor();
        initToolbar();
        initWebView();

        startEngkooWebView();
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
                        function.onCallBack(accessToken);
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
}
