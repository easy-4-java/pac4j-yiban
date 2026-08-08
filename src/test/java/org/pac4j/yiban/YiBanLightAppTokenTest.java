package org.pac4j.yiban;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class YiBanLightAppTokenTest {

    @Test
    void shouldStoreRawResponseViaConstructor() throws Exception {
        Constructor<YiBanLightAppToken> ctor = YiBanLightAppToken.class.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        YiBanLightAppToken token = ctor.newInstance("test-access-token");
        assertEquals("test-access-token", token.getRawResponse());
    }

    @Test
    void shouldImplementSerializable() throws Exception {
        Constructor<YiBanLightAppToken> ctor = YiBanLightAppToken.class.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        YiBanLightAppToken token = ctor.newInstance("my-token");
        assertInstanceOf(java.io.Serializable.class, token);
    }
}
