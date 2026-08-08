package org.pac4j.yiban;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpCommunicationException;
import org.pac4j.test.context.MockWebContext;
import org.pac4j.test.context.session.MockSessionStore;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class YiBanLightAppTokenAuthenticatorTest {

    private static final String APP_ID = "1234567890abcdef";
    private static final String APP_SECRET = "fedcba0987654321";

    @Test
    void shouldStoreAppIdAndAppSecret() {
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator("myAppId", "mySecret");
        assertEquals("myAppId", auth.getAppId());
        assertEquals("mySecret", auth.getAppSecret());
    }

    @Test
    void shouldThrowWhenCredentialsNull() {
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(APP_ID, APP_SECRET);
        CallContext ctx = new CallContext(MockWebContext.create(), new MockSessionStore());
        auth.init();

        assertThrows(CredentialsException.class, () -> auth.validate(ctx, null));
    }

    @Test
    void shouldThrowWhenCredentialsNotTokenCredentials() {
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(APP_ID, APP_SECRET);
        CallContext ctx = new CallContext(MockWebContext.create(), new MockSessionStore());
        auth.init();

        // Use a concrete non-TokenCredentials subclass instead of mocking
        Credentials fakeCreds = new Credentials() {};
        assertThrows(CredentialsException.class, () -> auth.validate(ctx, fakeCreds));
    }

    @Test
    void shouldThrowWhenVerifyRequestIsBlank() {
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(APP_ID, APP_SECRET);
        CallContext ctx = new CallContext(MockWebContext.create(), new MockSessionStore());
        auth.init();

        TokenCredentials tc = new TokenCredentials("  ");
        assertThrows(CredentialsException.class, () -> auth.validate(ctx, tc));
    }

    @Test
    void shouldThrowWhenVerifyRequestDecryptionFails() {
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(APP_ID, APP_SECRET);
        CallContext ctx = new CallContext(MockWebContext.create(), new MockSessionStore());
        auth.init();

        // "zzzz" is not valid hex -> AESDecoder.hexToBin will throw NumberFormatException
        TokenCredentials tc = new TokenCredentials("zzzz");
        assertThrows(CredentialsException.class, () -> auth.validate(ctx, tc));
    }

    @Test
    void shouldThrowWhenUserNotAuthorised() throws Exception {
        String appId = APP_ID;
        String appSecret = APP_SECRET;
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(appId, appSecret);
        CallContext ctx = new CallContext(MockWebContext.create(), new MockSessionStore());
        auth.init();

        // Build a JSON with visit_oauth = false (as a string)
        String json = "{\"visit_time\":123,\"visit_user\":{\"userid\":\"123\"},\"visit_oauth\":false}";
        String encrypted = aesEncrypt(json, appSecret, appId);

        TokenCredentials tc = new TokenCredentials(encrypted);
        assertThrows(CredentialsException.class, () -> auth.validate(ctx, tc));
    }

    @Test
    void shouldValidateSuccessfullyWithAuthorisedUser() throws Exception {
        String appId = APP_ID;
        String appSecret = APP_SECRET;
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(appId, appSecret);
        CallContext ctx = new CallContext(MockWebContext.create(), new MockSessionStore());
        auth.init();

        // Build a JSON with visit_oauth containing access_token
        JSONObject oauthObj = new JSONObject();
        oauthObj.put("access_token", "real-access-token-123");
        oauthObj.put("token_expires", "9999999999");
        JSONObject root = new JSONObject();
        root.put("visit_time", 123);
        root.put("visit_user", JSON.parse("{\"userid\":\"7400172\"}"));
        root.put("visit_oauth", oauthObj);
        String json = root.toJSONString();
        String encrypted = aesEncrypt(json, appSecret, appId);

        // Build the real_me response body
        String realMeBody = "{\"status\":\"success\",\"info\":{"
                + "\"yb_userid\":\"7400172\","
                + "\"yb_username\":\"Zhang San\","
                + "\"yb_studentid\":\"41364\","
                + "\"yb_usernick\":\"Zhang\","
                + "\"yb_sex\":\"M\""
                + "}}";

        // Mock HttpURLConnection
        HttpURLConnection mockConn = mock(HttpURLConnection.class);
        when(mockConn.getResponseCode()).thenReturn(200);
        when(mockConn.getInputStream()).thenReturn(new ByteArrayInputStream(realMeBody.getBytes()));

        try (var mockedHttpUtils2 = mockStatic(org.pac4j.core.util.HttpUtils2.class);
             var mockedHttpUtils = mockStatic(org.pac4j.core.util.HttpUtils.class)) {

            mockedHttpUtils2.when(() -> org.pac4j.core.util.HttpUtils2.openGetConnection(any(java.net.URL.class)))
                    .thenReturn(mockConn);
            mockedHttpUtils.when(() -> org.pac4j.core.util.HttpUtils.readBody(mockConn))
                    .thenReturn(realMeBody);

            TokenCredentials tc = new TokenCredentials(encrypted);
            Optional<Credentials> result = auth.validate(ctx, tc);

            assertTrue(result.isPresent());
            TokenCredentials resultCreds = (TokenCredentials) result.get();
            assertNotNull(resultCreds.getUserProfile());
            assertEquals("41364", resultCreds.getUserProfile().getId());
        }
    }

    @Test
    void shouldReturnAccessTokenFromCredentials() throws Exception {
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(APP_ID, APP_SECRET);
        TokenCredentials tc = new TokenCredentials("my-token-123");

        YiBanLightAppToken token = auth.getAccessToken(tc);
        assertNotNull(token);
        assertEquals("my-token-123", token.getRawResponse());
    }

    @Test
    void shouldThrowHttpCommunicationExceptionOnIoError() throws Exception {
        String appId = APP_ID;
        String appSecret = APP_SECRET;
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(appId, appSecret);
        CallContext ctx = new CallContext(MockWebContext.create(), new MockSessionStore());
        auth.init();

        // Build valid encrypted token
        JSONObject oauthObj = new JSONObject();
        oauthObj.put("access_token", "tok123");
        oauthObj.put("token_expires", "9999999999");
        JSONObject root = new JSONObject();
        root.put("visit_time", 123);
        root.put("visit_user", JSON.parse("{\"userid\":\"1\"}"));
        root.put("visit_oauth", oauthObj);
        String encrypted = aesEncrypt(root.toJSONString(), appSecret, appId);

        try (var mockedHttpUtils2 = mockStatic(org.pac4j.core.util.HttpUtils2.class)) {
            mockedHttpUtils2.when(() -> org.pac4j.core.util.HttpUtils2.openGetConnection(any(java.net.URL.class)))
                    .thenThrow(new java.io.IOException("Connection refused"));

            TokenCredentials tc = new TokenCredentials(encrypted);
            assertThrows(HttpCommunicationException.class, () -> auth.validate(ctx, tc));
        }
    }

    @Test
    void shouldParseVerifyRequestViaReflection() throws Exception {
        String appId = APP_ID;
        String appSecret = APP_SECRET;
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(appId, appSecret);

        String json = "{\"visit_time\":123,\"visit_oauth\":{\"access_token\":\"tok\"}}";
        String encrypted = aesEncrypt(json, appSecret, appId);

        Method parseMethod = YiBanLightAppTokenAuthenticator.class.getDeclaredMethod("parse", TokenCredentials.class);
        parseMethod.setAccessible(true);

        TokenCredentials tc = new TokenCredentials(encrypted);
        JSONObject result = (JSONObject) parseMethod.invoke(auth, tc);
        assertNotNull(result);
        assertNotNull(result.getJSONObject("visit_oauth"));
        assertEquals("tok", result.getJSONObject("visit_oauth").getString("access_token"));
    }

    @Test
    void shouldThrowViaParseWhenTokenBlank() throws Exception {
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(APP_ID, APP_SECRET);
        Method parseMethod = YiBanLightAppTokenAuthenticator.class.getDeclaredMethod("parse", TokenCredentials.class);
        parseMethod.setAccessible(true);

        TokenCredentials tc = new TokenCredentials("  ");
        try {
            parseMethod.invoke(auth, tc);
            fail("Should have thrown");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof CredentialsException);
        }
    }

    @Test
    void shouldInitializeProfileDefinition() {
        YiBanLightAppTokenAuthenticator auth = new YiBanLightAppTokenAuthenticator(APP_ID, APP_SECRET);
        auth.init();
        assertNotNull(auth.getProfileDefinition());
    }

    /**
     * Helper: encrypt plaintext with AES/CBC/NoPadding using the given key and iv.
     */
    private static String aesEncrypt(String plaintext, String key, String iv) throws Exception {
        // Pad plaintext to 16-byte boundary (NoPadding requires block alignment)
        int padLen = 16 - (plaintext.length() % 16);
        if (padLen < 16) {
            plaintext = plaintext + " ".repeat(padLen);
        }

        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : encrypted) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
