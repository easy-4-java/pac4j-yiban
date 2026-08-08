package org.pac4j.yiban;

import org.junit.jupiter.api.Test;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.test.context.MockWebContext;
import org.pac4j.test.context.session.MockSessionStore;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class YiBanTokenParameterExtractorTest {

    private static CallContext callContext(String method) {
        return new CallContext(MockWebContext.create().setRequestMethod(method), new MockSessionStore());
    }

    private static CallContext callContextWithParam(String method, String paramName, String paramValue) {
        MockWebContext ctx = MockWebContext.create()
                .setRequestMethod(method)
                .addRequestParameter(paramName, paramValue);
        return new CallContext(ctx, new MockSessionStore());
    }

    @Test
    void shouldExtractTokenFromGetRequest() {
        YiBanTokenParameterExtractor extractor = new YiBanTokenParameterExtractor("verify_request", true, false);
        CallContext ctx = callContextWithParam("GET", "verify_request", "abc123");

        Optional<Credentials> result = extractor.extract(ctx);

        assertTrue(result.isPresent());
        TokenCredentials tc = (TokenCredentials) result.get();
        assertEquals("abc123", tc.getToken());
    }

    @Test
    void shouldExtractTokenFromPostRequest() {
        YiBanTokenParameterExtractor extractor = new YiBanTokenParameterExtractor("verify_request", false, true);
        CallContext ctx = callContextWithParam("POST", "verify_request", "post-token");

        Optional<Credentials> result = extractor.extract(ctx);

        assertTrue(result.isPresent());
        TokenCredentials tc = (TokenCredentials) result.get();
        assertEquals("post-token", tc.getToken());
    }

    @Test
    void shouldReturnEmptyWhenParameterMissing() {
        YiBanTokenParameterExtractor extractor = new YiBanTokenParameterExtractor("verify_request", true, true);
        CallContext ctx = callContext("GET");

        Optional<Credentials> result = extractor.extract(ctx);
        assertFalse(result.isPresent());
    }

    @Test
    void shouldThrowWhenGetNotSupported() {
        YiBanTokenParameterExtractor extractor = new YiBanTokenParameterExtractor("verify_request", false, true);
        CallContext ctx = callContext("GET");

        assertThrows(CredentialsException.class, () -> extractor.extract(ctx));
    }

    @Test
    void shouldThrowWhenPostNotSupported() {
        YiBanTokenParameterExtractor extractor = new YiBanTokenParameterExtractor("verify_request", true, false);
        CallContext ctx = callContext("POST");

        assertThrows(CredentialsException.class, () -> extractor.extract(ctx));
    }

    @Test
    void shouldCreateWithSingleArgConstructor() {
        YiBanTokenParameterExtractor extractor = new YiBanTokenParameterExtractor("token");
        assertNotNull(extractor);
        assertEquals("token", extractor.getParameterName());
    }

    @Test
    void shouldCreateWithThreeArgConstructor() {
        YiBanTokenParameterExtractor extractor = new YiBanTokenParameterExtractor("token", true, true);
        assertNotNull(extractor);
        assertTrue(extractor.isSupportGetRequest());
        assertTrue(extractor.isSupportPostRequest());
    }

    @Test
    void shouldCreateWithFourArgConstructor() {
        YiBanTokenParameterExtractor extractor = new YiBanTokenParameterExtractor("token", false, true, "UTF-8");
        assertNotNull(extractor);
        assertFalse(extractor.isSupportGetRequest());
        assertTrue(extractor.isSupportPostRequest());
    }
}
