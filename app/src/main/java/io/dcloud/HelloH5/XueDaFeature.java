package io.dcloud.HelloH5;

import android.content.Intent;

import org.json.JSONArray;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.util.JSUtil;

public class XueDaFeature extends StandardFeature {
    private static final int STARTENGKOOWEBVIEW_REQUESTCODE = 0;
    private static final int STARTLOGINDETECTFACE_REQUESTCODE = 1;

    //启动小英
    public void startEngkooWebView(IWebview webview, JSONArray array) {

        final String callBackID = array.optString(0);
        final String url = array.optString(1);
        final String accessToken = array.optString(2);
        final String title = array.optString(3);

        Intent intent = new Intent(webview.getActivity(), EngkooActivity.class);
        intent.putExtra("EnglishAssistantScenarioLessonUrl", url);
        intent.putExtra("AccessToken", accessToken);
        intent.putExtra("Title", title);

        webview.getActivity().startActivityForResult(intent, STARTENGKOOWEBVIEW_REQUESTCODE);
    }

    public void startLoginDetectFace(final IWebview webview, JSONArray array) {

        final String callBackID = array.optString(0);

        Intent intent = new Intent(webview.getActivity(), LoginDetectFaceActivity.class);
        webview.getActivity().startActivityForResult(intent, STARTLOGINDETECTFACE_REQUESTCODE);

        final IApp _app = webview.obtainFrameView().obtainApp();
        _app.registerSysEventListener(new ISysEventListener() {
            @Override
            public boolean onExecute(SysEventType pEventType, Object pArgs) {

                Object[] _args = (Object[]) pArgs;
                int requestCode = (Integer) _args[0];
                Intent data = (Intent) _args[2];

                if (pEventType == SysEventType.onActivityResult) {
                    _app.unregisterSysEventListener(this, SysEventType.onActivityResult);

                    //判断请求码
                    if (requestCode == STARTLOGINDETECTFACE_REQUESTCODE) {
                        //获取返回值
                        String returnData = data.getStringExtra("return_data");
                        //执行 js 回调
                        JSUtil.execCallback(webview, callBackID, returnData, JSUtil.OK, false);
                    }
                }
                return false;
            }
        }, SysEventType.onActivityResult);
    }
}