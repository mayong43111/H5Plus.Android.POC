package io.dcloud.HelloH5;

import android.content.Intent;

import org.json.JSONArray;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;

public class EngkooWebViewFeature extends StandardFeature {

    public void startEngkooWebView(IWebview webview, JSONArray array) {

        final String callBackID = array.optString(0);
        final String url = array.optString(1);
        final String accessToken = array.optString(2);
        final String title = array.optString(3);
        final String nameMapping = array.optString(4);

        Intent intent = new Intent(webview.getActivity(), EngkooActivity.class);
        intent.putExtra("EnglishAssistantScenarioLessonUrl", url);
        intent.putExtra("AccessToken", accessToken);
        intent.putExtra("Title", title);
        intent.putExtra("NameMapping", nameMapping);

        webview.getActivity().startActivityForResult(intent, 0);
    }
}
