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

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.ext.client.TokenClient;
import org.pac4j.core.ext.profile.creator.TokenProfileCreator;
import org.pac4j.core.util.CommonHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * YiBan light-application direct client.
 *
 * <p>This client handles YiBan light-application authentication by reading
 * the {@code verify_request} parameter from the HTTP request, validating it
 * through the {@link YiBanLightAppTokenAuthenticator}, and creating a
 * {@link YiBanLightAppTokenProfile}.</p>
 *
 * <p>The login URL returned by {@link #getLoginUrl()} points to the YiBan
 * OAuth authorisation page with the configured {@code client_id}.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see YiBanLightAppTokenAuthenticator
 * @see YiBanTokenParameterExtractor
 */
@SuppressWarnings("all")
public class YiBanLightAppTokenClient extends TokenClient<YiBanLightAppTokenProfile, YiBanLightAppToken> {

    /** The application's client_id (AppID) registered on the YiBan platform. */
    private String appId;

    /**
     * Create a client with the given application ID.
     *
     * @param appId the YiBan application client_id
     */
    public YiBanLightAppTokenClient(String appId) {
        this.appId = appId;
    }

    /**
     * Initialise the client by wiring up the default profile creator and
     * credential extractor.
     *
     * @param forceReinit whether to force re-initialisation
     */
    @Override
    protected void internalInit(boolean forceReinit) {
        setProfileCreatorIfUndefined(new TokenProfileCreator());
        setCredentialsExtractorIfUndefined(new YiBanTokenParameterExtractor(this.getParameterName(), this.isSupportGetRequest(), this.isSupportPostRequest()));
        // ensures components have been properly initialized
        CommonHelper.assertNotNull("credentialsExtractor", getCredentialsExtractor());
        CommonHelper.assertNotNull("authenticator", getAuthenticator());
        CommonHelper.assertNotNull("profileCreator", getProfileCreator());
    }

    /**
     * Return the YiBan OAuth login URL.
     *
     * <p>The URL points to {@code https://oauth.yiban.cn/code/html} with the
     * configured {@code client_id} and the callback URL (URL-encoded).</p>
     *
     * @return the full YiBan OAuth authorisation URL, or {@code null} if
     *         URL-encoding fails
     */
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

    /**
     * Return the YiBan application client_id.
     *
     * @return the application ID
     */
    public String getAppId() {
        return appId;
    }
}
