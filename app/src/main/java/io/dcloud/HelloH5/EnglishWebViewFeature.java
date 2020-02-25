package io.dcloud.HelloH5;

import android.content.Intent;

import org.json.JSONArray;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;

public class EnglishWebViewFeature extends StandardFeature {

    public void startEnglishWebView(IWebview webview, JSONArray array) {

        final String CallBackID = array.optString(0);
        final String Url = array.optString(1);
        final String AccessToken = array.optString(2);

        Intent intent = new Intent(webview.getActivity(), EnglishActivity.class);
        intent.putExtra("EnglishAssistantScenarioLessonUrl", Url);
        intent.putExtra("AccessToken", AccessToken);
        webview.getActivity().startActivityForResult(intent, 1);
    }
}
