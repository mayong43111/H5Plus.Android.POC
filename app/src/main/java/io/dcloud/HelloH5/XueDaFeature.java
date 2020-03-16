package io.dcloud.HelloH5;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.ThreadPool;
import io.dcloud.feature.barcode2.camera.CameraManager;

public class XueDaFeature extends StandardFeature {
    private static final int STARTENGKOOWEBVIEW_REQUESTCODE = 0;
    private static final int STARTLOGINDETECTFACE_REQUESTCODE = 1;
    private static final String IMAGE_FILE_NAME = "pic_xxxxx.jpg";

    //启动小英
    public void startEngkooWebView(final IWebview webview, JSONArray array) {

        final String callBackID = array.optString(0);
        final String url = array.optString(1);
        final String accessToken = array.optString(2);
        final String title = array.optString(3);

        final IApp _app = webview.obtainFrameView().obtainApp();
        PermissionUtil.usePermission(_app.getActivity(), _app.isStreamApp(), PermissionUtil.PMS_CAMERA, new PermissionUtil.StreamPermissionRequest(_app) {

            @Override
            public void onGranted(String s) {
                Intent intent = new Intent(webview.getActivity(), EngkooActivity.class);
                intent.putExtra("EnglishAssistantScenarioLessonUrl", url);
                intent.putExtra("AccessToken", accessToken);
                intent.putExtra("Title", title);

                webview.getActivity().startActivityForResult(intent, STARTENGKOOWEBVIEW_REQUESTCODE);
            }

            @Override
            public void onDenied(String s) {

            }
        });
    }

    public void startLoginDetectFace(final IWebview webview, JSONArray array) {

        final String callBackID = array.optString(0);
        final IApp _app = webview.obtainFrameView().obtainApp();

        //Android 7.0 FileUriExposedException 解决
        if (Build.VERSION.SDK_INT >= 24) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        PermissionUtil.usePermission(_app.getActivity(), _app.isStreamApp(), PermissionUtil.PMS_CAMERA, new PermissionUtil.StreamPermissionRequest(_app) {
            @Override
            public void onGranted(String s) {
                try {
                    final File destFile = new File(Environment.getExternalStorageDirectory(), IMAGE_FILE_NAME);

                    File parentFile = destFile.getParentFile();
                    if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    _app.registerSysEventListener(new ISysEventListener() {
                        @Override
                        public boolean onExecute(SysEventType pEventType, Object pArgs) {
                            Object[] _args = (Object[]) pArgs;
                            int requestCode = (Integer) _args[0];
                            int resultCode = (Integer) _args[1];
                            if (pEventType == SysEventType.onActivityResult) {
                                if (requestCode == STARTLOGINDETECTFACE_REQUESTCODE) {
                                    ThreadPool.self().addThreadTask(new Runnable() {
                                        @Override
                                        public void run() {
                                            _app.getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    JSUtil.execCallback(webview, callBackID, File2String(destFile), JSUtil.OK, false);
                                                }
                                            });
                                        }
                                    });
                                    _app.unregisterSysEventListener(this, pEventType);
                                }
                            }
                            return false;
                        }
                    }, SysEventType.onActivityResult);
                    Intent _intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    Uri _uri = Uri.fromFile(destFile);
                    _intent.putExtra(MediaStore.EXTRA_OUTPUT, _uri);
                    _intent.putExtra("android.intent.extras.CAMERA_FACING", 1);//前置摄像头
                    webview.getActivity().startActivityForResult(_intent, STARTLOGINDETECTFACE_REQUESTCODE);
                } catch (Exception e) {
                }
            }

            @Override
            public void onDenied(String s) {

            }
        });
    }

    private static String File2String(File file) {
        byte[] buffer = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return android.util.Base64.encodeToString(buffer, Base64.NO_WRAP);
    }
}