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
import java.util.Optional;

import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
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
 * Authenticator for YiBan light-application tokens.
 *
 * <p>This authenticator decrypts the {@code verify_request} parameter using
 * AES, checks whether the user has authorised the application, extracts the
 * OAuth access token, and then calls the YiBan {@code /user/real_me} endpoint
 * to retrieve the user's real-name profile.</p>
 *
 * <p>Decrypted JSON structure:</p>
 * <pre>{@code
 * {
 *   "visit_time": 1234567890,
 *   "visit_user": { "userid": "..." },
 *   "visit_oauth": {
 *     "access_token": "...",
 *     "token_expires": 1234567890
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see YiBanLightAppTokenClient
 * @see YiBanLightAppTokenProfileDefinition
 * @see AESDecoder
 */
@SuppressWarnings("all")
public class YiBanLightAppTokenAuthenticator extends TokenAuthenticator<YiBanLightAppTokenProfile, YiBanLightAppToken> {

    /** URL of the YiBan real-name user info endpoint. */
    private String realMe = "https://openapi.yiban.cn/user/real_me";

    /** The application's AppID registered on the YiBan platform. */
    private String appId;

    /** The application's AppSecret registered on the YiBan platform. */
    private String appSecret;

    /**
     * Create an authenticator with the given application credentials.
     *
     * @param appId     the YiBan application ID
     * @param appSecret the YiBan application secret
     */
    public YiBanLightAppTokenAuthenticator(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    /**
     * Initialise the authenticator by setting the default profile definition.
     *
     * @param forceReinit whether to force re-initialisation even if already
     *                    initialised
     */
    @Override
    protected void internalInit(boolean forceReinit) {
        defaultProfileDefinition(new YiBanLightAppTokenProfileDefinition(x -> new YiBanLightAppTokenProfile()));
        super.internalInit(forceReinit);
    }

    /**
     * Validate the token credentials by decrypting the YiBan
     * {@code verify_request}, checking authorisation, and fetching the user
     * profile.
     *
     * @param callContext the call context containing the web context
     * @param credentials the raw credentials (must be {@link TokenCredentials})
     * @return an {@link Optional} containing the credentials with the user
     *         profile set
     * @throws CredentialsException if the credentials are missing, the user
     *         has not authorised the application, or the token is invalid
     */
    @Override
    public Optional<Credentials> validate(CallContext callContext, Credentials credentials) {
        if (credentials == null) {
            throw new CredentialsException("No credential");
        }
        if (!(credentials instanceof TokenCredentials)) {
            throw new CredentialsException("Unsupported credential type: " + credentials.getClass().getName());
        }
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;

        JSONObject jsonObject = parse(tokenCredentials);
        String visitOauth = jsonObject.getString("visit_oauth");
        if ("false".equals(visitOauth)) {
            throw new CredentialsException("YiBan light-app user has not authorised");
        }
        // User has authorised -- fetch real-name info
        JSONObject oauthObject = jsonObject.getJSONObject("visit_oauth");
        CommonHelper.assertNotNull("oauthObject", oauthObject);
        String accessToken = oauthObject.getString("access_token");
        CommonHelper.assertNotNull("YiBan light-app accessToken", accessToken);
        String body = retrieveUserProfileFromRestApi(callContext.webContext(), new YiBanLightAppToken(accessToken), realMe);
        CommonHelper.assertNotNull("YiBan light-app user profile response", body);
        logger.info("body:{}", body);
        final YiBanLightAppTokenProfile profile = getProfileDefinition().extractUserProfile(body);
        logger.debug("profile: {}", profile);
        tokenCredentials.setUserProfile(profile);
        return Optional.of(tokenCredentials);
    }

    /**
     * Retrieve the user profile from the YiBan REST API.
     *
     * <p>This override appends the {@code access_token} as a query parameter
     * and performs a simple GET request (without custom headers/params).</p>
     *
     * @param context     the web context
     * @param accessToken the YiBan access token
     * @param profileUrl  the REST API URL
     * @return the response body, or {@code null} on authentication or
     *         unexpected HTTP errors
     * @throws HttpCommunicationException if an I/O error occurs
     */
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

    /**
     * Create a {@link YiBanLightAppToken} from the given credentials.
     *
     * @param credentials the token credentials
     * @return a new {@link YiBanLightAppToken} wrapping the raw token string
     */
    @Override
    protected YiBanLightAppToken getAccessToken(TokenCredentials credentials) {
        return new YiBanLightAppToken(credentials.getToken());
    }

    /**
     * Parse and decrypt the {@code verify_request} token into a JSON object.
     *
     * @param credentials the token credentials containing the encrypted
     *                    verify_request
     * @return the decrypted JSON object
     * @throws CredentialsException if the token is blank or decryption fails
     */
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

    /**
     * Return the YiBan application ID.
     *
     * @return the application ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Return the YiBan application secret.
     *
     * @return the application secret
     */
    public String getAppSecret() {
        return appSecret;
    }
}
