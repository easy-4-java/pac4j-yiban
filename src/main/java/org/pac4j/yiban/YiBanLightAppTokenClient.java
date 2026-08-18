/*
 * Copyright (c) 2018, hiwepy (https://github.com/easy-4-java).
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

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.ext.client.TokenClient;
import org.pac4j.core.ext.profile.creator.TokenProfileCreator;
import org.pac4j.core.util.CommonHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 易班轻应用认证登录客户端
 * @author zd
 */
@SuppressWarnings("all")
public class YiBanLightAppTokenClient extends TokenClient<YiBanLightAppTokenProfile, YiBanLightAppToken> {

    /**
     * 应用的AppID
     */
    private String appId;

    public YiBanLightAppTokenClient(String appId) {
        this.appId = appId;
    }

    @Override
    protected void clientInit() {
        defaultProfileCreator(new TokenProfileCreator());
        defaultCredentialsExtractor(new YiBanTokenParameterExtractor(this.getParameterName(), this.isSupportGetRequest(), this.isSupportPostRequest()));
        // ensures components have been properly initialized
        CommonHelper.assertNotNull("credentialsExtractor", getCredentialsExtractor());
        CommonHelper.assertNotNull("authenticator", getAuthenticator());
        CommonHelper.assertNotNull("profileCreator", getProfileCreator());
    }

    @Override
    public String getLoginUrl() {
        try {
            return StringUtils.join("https://oauth.yiban.cn/code/html?client_id=", getAppId(), "&redirect_uri=",
                    URLEncoder.encode(super.getLoginUrl(), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAppId() {
        return appId;
    }
}
