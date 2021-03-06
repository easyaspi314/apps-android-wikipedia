package org.wikipedia.createaccount;

import android.content.Context;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.CaptchaResult;

public abstract class CreateAccountTask extends ApiTask<CreateAccountResult> {
    private final String username;
    private final String password;
    private final String email;

    private String token;

    public CreateAccountTask(Context context, String username, String password, String email) {
        super(SINGLE_THREAD, ((WikipediaApp)context.getApplicationContext()).getPrimarySiteApi());

        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        RequestBuilder builder = api.action("createaccount")
                .param("name", username)
                .param("password", password);
        if (email != null) {
            builder.param("email", email);
        }
        if (token != null) {
            builder.param("token", token);
        }
        return builder;
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post();
    }

    @Override
    public CreateAccountResult processResult(ApiResult result) throws Throwable {
        if (result.asObject().has("error")) {
            return new CreateAccountResult(result.asObject().optJSONObject("error").optString("code"));
        }
        JSONObject ca = result.asObject().optJSONObject("createaccount");
        String apiResult = ca.optString("result");
        if (apiResult.equals("NeedToken")) {
            // We need to just repeat the request.
            // Set token and restart the request
            token = ca.optString("token");
            return performTask();
        } else if (apiResult.equals("NeedCaptcha")) {
            return new CreateAccountCaptchaResult(new CaptchaResult(ca.optJSONObject("captcha").optString("id")));
        } else if (apiResult.equals("Success")) {
            return new CreateAccountSuccessResult(ca.optString("username"));
        } else {
            return new CreateAccountResult(apiResult);
        }
    }
}
