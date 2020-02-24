package io.dcloud.HelloH5;

import android.content.Intent;

import org.json.JSONArray;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;

    public class EnglishWebViewFeature extends StandardFeature {

    public void startEnglishWebView(IWebview webview, JSONArray jsonArray) {

        Intent intent = new Intent(webview.getActivity(), EnglishActivity.class);
        webview.getActivity().startActivityForResult(intent, 1);
    }
}
