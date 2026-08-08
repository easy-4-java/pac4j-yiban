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

import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.ext.profile.TokenProfileDefinition;
import org.pac4j.core.profile.factory.ProfileFactory;
import org.pac4j.core.util.CommonHelper;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * Profile definition that extracts a {@link YiBanLightAppTokenProfile} from
 * the JSON response body returned by the YiBan {@code /user/real_me} endpoint.
 *
 * <p>Expected JSON structure:</p>
 * <pre>{@code
 * {
 *   "status": "success",
 *   "info": {
 *     "yb_userid": "7400172",
 *     "yb_username": "Zhang San",
 *     "yb_studentid": "41364",
 *     ...
 *   }
 * }
 * }</pre>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see YiBanLightAppTokenProfile
 * @see YiBanLightAppTokenAuthenticator
 */
@Slf4j
@SuppressWarnings("all")
public class YiBanLightAppTokenProfileDefinition extends TokenProfileDefinition<YiBanLightAppTokenProfile, YiBanLightAppToken> {

    /**
     * Create a profile definition with the default profile factory.
     */
    public YiBanLightAppTokenProfileDefinition() {
        super();
    }

    /**
     * Create a profile definition with a custom profile factory.
     *
     * @param profileFactory the factory used to instantiate profile objects
     */
    public YiBanLightAppTokenProfileDefinition(final ProfileFactory profileFactory) {
        super(profileFactory);
    }

    /**
     * Return the profile URL for the given access token.
     *
     * <p>This implementation always returns {@code null} because the
     * authenticator hard-codes the YiBan {@code /user/real_me} URL.</p>
     *
     * @param webContext the current web context (unused)
     * @param yiBanLightAppToken the access token (unused)
     * @return always {@code null}
     */
    @Override
    public String getProfileUrl(WebContext webContext, YiBanLightAppToken yiBanLightAppToken) {
        return null;
    }

    /**
     * Extract a {@link YiBanLightAppTokenProfile} from the JSON response body
     * returned by the YiBan real-name API.
     *
     * @param body the JSON response body from YiBan
     * @return the populated user profile
     * @throws CredentialsException if the response status is not
     *         {@code "success"} or required fields are missing
     */
    @Override
    public YiBanLightAppTokenProfile extractUserProfile(String body) {
        String success = "success";
        String status = "status";
        String studentIdStr = "yb_studentid";
        String usernameStr = "yb_username";
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        JSONObject realMeBody = JSONObject.parseObject(body);
        if (success.equals(realMeBody.getString(status))) {
            JSONObject infoObject = realMeBody.getJSONObject("info");
            CommonHelper.assertNotNull("YiBan profile info object", infoObject);
            log.info("YiBan profile info: {}", infoObject);
            String studentId = infoObject.getString(studentIdStr);
            CommonHelper.assertNotNull("YiBan student ID", studentId);
            log.info("YiBan student ID: {}", studentId);
            String username = infoObject.getString(usernameStr);
            CommonHelper.assertNotNull("YiBan username", username);
            log.info("YiBan username: {}", username);
            profile.setPid(studentId);
            profile.setId(studentId);
            profile.setUserid(studentId);
            profile.setXm(username);
        } else {
            throw new CredentialsException(realMeBody.getJSONObject("info").getString("msgCN"));
        }
        logger.debug("profile: {}", profile);
        return profile;
    }

}
