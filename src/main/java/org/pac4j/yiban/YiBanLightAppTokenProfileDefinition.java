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

import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.ext.profile.TokenProfileDefinition;
import org.pac4j.core.profile.factory.ProfileFactory;
import org.pac4j.core.util.CommonHelper;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * Access Token profile definition.
 *
 * @author zd
 */
@Slf4j
@SuppressWarnings("all")
public class YiBanLightAppTokenProfileDefinition extends TokenProfileDefinition<YiBanLightAppTokenProfile, YiBanLightAppToken> {

    public YiBanLightAppTokenProfileDefinition() {
        super();
    }

    public YiBanLightAppTokenProfileDefinition(final ProfileFactory<YiBanLightAppTokenProfile> profileFactory) {
        super(profileFactory);
    }


    @Override
    public String getProfileUrl(WebContext webContext, YiBanLightAppToken yiBanLightAppToken) {
        return null;
    }

    /**
     * Extract the user profile from the response (JSON, XML...) of the profile url.
     * TODO  解析用户属性信息
     *
     * @param body the response body
     * @return the returned profile
     */
    @Override
    public YiBanLightAppTokenProfile extractUserProfile(String body) {
        String success = "success";
        String status = "status";
        String studentIdStr = "yb_studentid";
        String usernameStr = "yb_username";
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        /*
        {"status":"success","info":
            {"yb_userid":"7400172","yb_username":"\u91d1\u9633","yb_usernick":"\u91d1\u9633",
            "yb_sex":"M","yb_money":"1251","yb_exp":"904","yb_userhead":"http:\/\/img02.fs.yiban.cn\/7400172\/avatar\/user\/200",
            "yb_schoolid":"34270","yb_schoolname":"\u676d\u5dde\u7535\u5b50\u79d1\u6280\u5927\u5b66\u4fe1\u606f\u5de5\u7a0b\u5b66\u9662",
            "yb_regtime":"2016-01-07 01:23:48","yb_realname":"\u91d1\u9633","yb_birthday":"1987-12-26","yb_studentid":"41364","yb_identity":"\u8f85\u5bfc\u5458"}
         }
         */
        JSONObject realMeBody = JSONObject.parseObject(body);
        if (success.equals(realMeBody.getString(status))) {
            JSONObject infoObject = realMeBody.getJSONObject("info");
            CommonHelper.assertNotNull("易班轻应用获取用户认证解析信息", infoObject);
            log.info("易班轻应用获取用户认证解析信息:{}",infoObject);
            String studentId = infoObject.getString(studentIdStr);
            CommonHelper.assertNotNull("易班轻应用获取用户学工号", studentId);
            log.info("易班轻应用获取用户学工号:{}",studentId);
            String username = infoObject.getString(usernameStr);
            CommonHelper.assertNotNull("易班轻应用获取用户实名认证名", username);
            log.info("易班轻应用获取用户实名认证名:{}",username);
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
