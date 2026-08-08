package org.pac4j.yiban;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YiBanLightAppTokenProfileTest {

    @Test
    void shouldReturnPidAsId() {
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        profile.setPid("S12345");
        assertEquals("S12345", profile.getId());
    }

    @Test
    void shouldReturnNullIdWhenPidNotSet() {
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        assertNull(profile.getId());
    }

    @Test
    void shouldStoreAndRetrieveUserid() {
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        profile.setUserid("7400172");
        assertEquals("7400172", profile.getUserid());
    }

    @Test
    void shouldStoreAndRetrieveXm() {
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        profile.setXm("Zhang San");
        assertEquals("Zhang San", profile.getXm());
    }

    @Test
    void shouldStoreAndRetrievePtype() {
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        profile.setPtype("student");
        assertEquals("student", profile.getPtype());
    }

    @Test
    void shouldStoreAndRetrieveCsrq() {
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        profile.setCsrq("2000-01-01");
        assertEquals("2000-01-01", profile.getCsrq());
    }

    @Test
    void shouldStoreAndRetrieveFlag() {
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        profile.setFlag("1");
        assertEquals("1", profile.getFlag());
    }

    @Test
    void shouldHaveCorrectEqualsAndHashCode() {
        YiBanLightAppTokenProfile p1 = new YiBanLightAppTokenProfile();
        p1.setPid("S001");
        p1.setUserid("U001");

        YiBanLightAppTokenProfile p2 = new YiBanLightAppTokenProfile();
        p2.setPid("S001");
        p2.setUserid("U001");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void shouldHaveCorrectToString() {
        YiBanLightAppTokenProfile profile = new YiBanLightAppTokenProfile();
        profile.setPid("S001");
        profile.setXm("TestUser");
        String str = profile.toString();
        assertNotNull(str);
        assertTrue(str.contains("S001"));
    }
}
