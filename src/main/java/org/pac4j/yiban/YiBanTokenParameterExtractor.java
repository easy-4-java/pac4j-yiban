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

import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.WebContextHelper;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.ext.credentials.extractor.TokenParameterExtractor;

import java.util.Optional;

/**
 * Credential extractor for the YiBan light-application platform.
 *
 * <p>This extractor reads the {@code verify_request} (or a custom parameter)
 * from the HTTP request and wraps it in a {@link TokenCredentials}. It
 * supports both GET and POST requests, configurable via the constructor.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see YiBanLightAppTokenClient
 * @see YiBanLightAppTokenAuthenticator
 */
@SuppressWarnings("all")
public class YiBanTokenParameterExtractor extends TokenParameterExtractor {

    /**
     * Create an extractor for the given parameter name with default settings.
     *
     * @param parameterName the request parameter name to read the token from
     */
    public YiBanTokenParameterExtractor(String parameterName) {
        super(parameterName);
    }

    /**
     * Create an extractor with explicit GET/POST support flags.
     *
     * @param parameterName     the request parameter name
     * @param supportGetRequest whether GET requests are supported
     * @param supportPostRequest whether POST requests are supported
     */
    public YiBanTokenParameterExtractor(String parameterName, boolean supportGetRequest, boolean supportPostRequest) {
        super(parameterName, supportGetRequest, supportPostRequest);
    }

    /**
     * Create an extractor with explicit GET/POST support flags and charset.
     *
     * @param parameterName     the request parameter name
     * @param supportGetRequest whether GET requests are supported
     * @param supportPostRequest whether POST requests are supported
     * @param charset           the character encoding to use
     */
    public YiBanTokenParameterExtractor(String parameterName, boolean supportGetRequest, boolean supportPostRequest, String charset) {
        super(parameterName, supportGetRequest, supportPostRequest, charset);
    }

    /**
     * Extract token credentials from the web context.
     *
     * <p>Validates that the HTTP method is allowed, then reads the token
     * parameter from the request.</p>
     *
     * @param callContext the call context containing the web context and session store
     * @return an {@link Optional} containing the extracted credentials, or
     *         {@link Optional#empty()} if the parameter is absent
     * @throws CredentialsException if the HTTP method is not supported
     */
    @Override
    public Optional<Credentials> extract(CallContext callContext) {
        WebContext context = callContext.webContext();
        logger.debug("supportGetRequest: {}", this.isSupportGetRequest());
        logger.debug("supportPostRequest: {}", this.isSupportPostRequest());
        if (WebContextHelper.isGet(context) && !isSupportGetRequest()) {
            throw new CredentialsException("GET requests not supported");
        } else if (WebContextHelper.isPost(context) && !isSupportPostRequest()) {
            throw new CredentialsException("POST requests not supported");
        }
        logger.debug("ParameterName: {}", this.getParameterName());
        Optional<String> value = context.getRequestParameter(this.getParameterName());
        if (!value.isPresent()) {
            return Optional.empty();
        }
        String tokenString = value.get();
        logger.debug("token : {}", tokenString);
        return Optional.of(new TokenCredentials(tokenString));
    }
}
