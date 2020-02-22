package io.dcloud.HelloH5;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

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
    private final Activity currentActivity;
    private final Application currentApplication;

    public static final MediaType HTTP_FORM
            = MediaType.parse("application/x-www-form-urlencoded");
    public static final int RC_RECORD_AUDIO = 100; //只要不重复就行
    public static final String EnglishAssistantGrantType = "XueLe";
    public static final String EnglishAssistantID = "useridxxxx";
    public static final String EnglishAssistantSecret = "2FDA0803-2202-40EB-824D-28CDDC3A2FE4";
    public static final String EnglishAssistantOAuthUrl = "https://app-staging.mtutor.engkoo.com/proxy/oauth/login";
    public static final String EnglishAssistantScenarioLessonUrl = "https://app-staging.mtutor.engkoo.com/dist/app-scenario-lesson/?origin=android-xinfangxiang";

    OkHttpClient httpClient = new OkHttpClient();

    public EnglishActivity() {
        currentActivity = this;
        currentApplication = this.getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_english);

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
                        try {
                            function.onCallBack(loadUserAccessToken());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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

    private String loadUserAccessToken() throws InterruptedException {

        RequestBody formBody = new FormEncodingBuilder()
                .add("grant_type", EnglishAssistantGrantType)
                .add("id", EnglishAssistantID) //TODO: 学员ID
                .add("secret", EnglishAssistantSecret)
                .build();

        Request request = new Request.Builder()
                .url(EnglishAssistantOAuthUrl)
                .post(formBody)
                .build();

        final Object object = new Object();
        synchronized (object) {
            final String[] access_token = new String[1];

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    synchronized (object) {
                        object.notify();
                    }
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if (response.isSuccessful()) {
                        access_token[0] = response.body().string();
                    }
                    synchronized (object) {
                        object.notify();
                    }
                }
            });
            object.wait();
            return access_token[0];
        }
    }
}
