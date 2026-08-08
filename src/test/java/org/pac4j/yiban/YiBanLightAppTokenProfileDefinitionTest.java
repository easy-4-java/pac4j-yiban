package org.pac4j.yiban;

import org.junit.jupiter.api.Test;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.test.context.MockWebContext;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class YiBanLightAppTokenProfileDefinitionTest {

    @Test
    void shouldReturnNullProfileUrl() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition();
        MockWebContext ctx = MockWebContext.create();
        Constructor<YiBanLightAppToken> ctor;
        try {
            ctor = YiBanLightAppToken.class.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            YiBanLightAppToken token = ctor.newInstance("test-token");
            assertNull(def.getProfileUrl(ctx, token));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void shouldExtractProfileFromSuccessBody() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition();
        String body = "{\"status\":\"success\",\"info\":{"
                + "\"yb_userid\":\"7400172\","
                + "\"yb_username\":\"Zhang San\","
                + "\"yb_studentid\":\"41364\","
                + "\"yb_usernick\":\"Zhang\","
                + "\"yb_sex\":\"M\""
                + "}}";

        YiBanLightAppTokenProfile profile = def.extractUserProfile(body);

        assertNotNull(profile);
        assertEquals("41364", profile.getPid());
        assertEquals("41364", profile.getId());
        assertEquals("41364", profile.getUserid());
        assertEquals("Zhang San", profile.getXm());
    }

    @Test
    void shouldExtractProfileWithMinimalInfo() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition();
        String body = "{\"status\":\"success\",\"info\":{"
                + "\"yb_studentid\":\"S001\","
                + "\"yb_username\":\"Li Si\""
                + "}}";

        YiBanLightAppTokenProfile profile = def.extractUserProfile(body);
        assertEquals("S001", profile.getPid());
        assertEquals("Li Si", profile.getXm());
    }

    @Test
    void shouldThrowCredentialsExceptionOnFailureStatus() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition();
        String body = "{\"status\":\"failure\",\"info\":{\"msgCN\":\"User not authorised\"}}";

        CredentialsException ex = assertThrows(CredentialsException.class, () -> def.extractUserProfile(body));
        assertTrue(ex.getMessage().contains("User not authorised"));
    }

    @Test
    void shouldThrowWhenInfoObjectMissing() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition();
        String body = "{\"status\":\"success\"}";

        assertThrows(Exception.class, () -> def.extractUserProfile(body));
    }

    @Test
    void shouldThrowWhenStudentIdMissing() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition();
        String body = "{\"status\":\"success\",\"info\":{\"yb_username\":\"Test\"}}";

        assertThrows(Exception.class, () -> def.extractUserProfile(body));
    }

    @Test
    void shouldThrowWhenUsernameMissing() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition();
        String body = "{\"status\":\"success\",\"info\":{\"yb_studentid\":\"S001\"}}";

        assertThrows(Exception.class, () -> def.extractUserProfile(body));
    }

    @Test
    void shouldCreateWithDefaultConstructor() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition();
        assertNotNull(def);
    }

    @Test
    void shouldCreateWithProfileFactory() {
        YiBanLightAppTokenProfileDefinition def = new YiBanLightAppTokenProfileDefinition(
                args -> new YiBanLightAppTokenProfile());
        assertNotNull(def);
    }
}
