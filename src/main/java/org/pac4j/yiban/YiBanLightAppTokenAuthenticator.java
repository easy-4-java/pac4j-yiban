/*
 * Copyright (c) 2018, Loong Wan (https://github.com/loong10k).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.pac4j.yiban;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpCommunicationException;
import org.pac4j.core.ext.credentials.authenticator.TokenAuthenticator;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.HttpUtils;
import org.pac4j.core.util.HttpUtils2;
import org.pac4j.yiban.utils.AESDecoder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 易班轻应用token认证解析
 */
@SuppressWarnings("all")
public class YiBanLightAppTokenAuthenticator extends TokenAuthenticator<YiBanLightAppTokenProfile, YiBanLightAppToken> {

    private String realMe = "https://openapi.yiban.cn/user/real_me";

    /**
     * 应用的AppID
     */
    private String appId;

    /**
     * 应用的AppSecret
     */
    private String appSecret;


    public YiBanLightAppTokenAuthenticator(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @Override
    protected void internalInit() {
        defaultProfileDefinition(new YiBanLightAppTokenProfileDefinition(x -> new YiBanLightAppTokenProfile()));
        super.internalInit();
    }

    @Override
    public void validate(final TokenCredentials credentials, final WebContext context) {
        String flag = "false";
        if (credentials == null) {
            throw new CredentialsException("No credential");
        }
		/*
		失败
		{
		  "visit_time":访问unix时间戳,
		  "visit_user":{
			"userid":"易班用户ID"
		  },
		  "visit_oauth":false
		}
		成功
		{
		  "visit_time":访问unix时间戳,
		  "visit_user":{
			"userid":"易班用户ID",
			"username":"易班用户名",
			"usernick":"易班用户昵称",
			"usersex":"易班用户性别"
		  },
		  "visit_oauth":{
			"access_token":"授权凭证",
			"token_expires":"有效unix时间戳"
		  },
		}
		 */
        JSONObject jsonObject = parse(credentials);
        String visitOauth = jsonObject.getString("visit_oauth");
        if (visitOauth.equals(flag)) {
            throw new CredentialsException("易班轻应用用户未授权");
        }
        //用户已授权,开始查询用户实名信息数据接口
        JSONObject oauthObject = jsonObject.getJSONObject("visit_oauth");
        CommonHelper.assertNotNull("oauthObject", oauthObject);
        String accessToken = oauthObject.getString("access_token");
        CommonHelper.assertNotNull("易班轻应用accessToken", accessToken);
        String body = retrieveUserProfileFromRestApi(context, new YiBanLightAppToken(accessToken), realMe);
        CommonHelper.assertNotNull("易班轻应用获取用户认证信息", body);
        logger.info("body:{}", body);
        final YiBanLightAppTokenProfile profile = getProfileDefinition().extractUserProfile(body);
        logger.debug("profile: {}", profile);
        credentials.setUserProfile(profile);
    }

    @Override
    protected String retrieveUserProfileFromRestApi(WebContext context, YiBanLightAppToken accessToken, String profileUrl) {
        logger.debug("accessToken: {} / profileUrl: {}", accessToken.getRawResponse(), profileUrl);
        final long t0 = System.currentTimeMillis();
        HttpURLConnection connection = null;
        try {
            String urlStr = CommonHelper.addParameter(profileUrl, "access_token", accessToken.getRawResponse());
            URL url = new URL(urlStr);
            logger.info("final Url:{}", urlStr);
            connection = HttpUtils2.openGetConnection(url);
            int code = connection.getResponseCode();
            final long t1 = System.currentTimeMillis();
            logger.debug("Request took: " + (t1 - t0) + " ms for: " + profileUrl);
            if (code == 200) {
                return HttpUtils.readBody(connection);
            } else if (code == 401 || code == 403) {
                logger.info("Authentication failure for token: {} -> {}", accessToken.getRawResponse(), HttpUtils.buildHttpErrorMessage(connection));
                return null;
            } else {
                logger.warn("Unexpected error for token: {} -> {}", accessToken.getRawResponse(), HttpUtils.buildHttpErrorMessage(connection));
                return null;
            }
        } catch (final IOException e) {
            throw new HttpCommunicationException("Error getting body: " + e.getMessage());
        } finally {
            HttpUtils.closeConnection(connection);
        }
    }

    @Override
    protected YiBanLightAppToken getAccessToken(TokenCredentials credentials) {
        return new YiBanLightAppToken(credentials.getToken());
    }

    private JSONObject parse(TokenCredentials credentials) {
        String verify_request = credentials.getToken();
        if (CommonHelper.isBlank(verify_request)) {
            throw new CredentialsException("verify_request cannot be blank");
        }
        String decString;
        try {
            decString = AESDecoder.dec(verify_request.trim(), getAppSecret().trim(), getAppId().trim());
        } catch (Exception e) {
            throw new CredentialsException(e);
        }
        CommonHelper.assertNotNull("decString", decString);
        JSONObject jsonObject = JSON.parseObject(decString);
        return jsonObject;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }
}
