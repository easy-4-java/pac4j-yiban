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

import org.pac4j.core.ext.profile.TokenProfile;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User profile for a YiBan light-application authenticated user.
 *
 * <p>After a successful authentication, this profile is populated with the
 * real-name information returned by the YiBan {@code /user/real_me} endpoint,
 * including the person-id ({@code pid}), name ({@code xm}), and other
 * attributes.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see YiBanLightAppTokenAuthenticator
 * @see YiBanLightAppTokenProfileDefinition
 */
@Data
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("all")
public class YiBanLightAppTokenProfile extends TokenProfile {

    /** YiBan user identifier. */
    private String userid;

    /** Unified person number (e.g. student/staff ID). */
    private String pid;

    /** Real name of the user. */
    private String xm;

    /** Person type: student or staff. */
    private String ptype;

    /** Date of birth. */
    private String csrq;

    /** Password initialisation flag: 0 = not initialised, 1 = initialised. */
    private String flag;

    /**
     * Return the unique identifier of this profile.
     *
     * <p>Overrides the default to return the person-id ({@code pid}), which is
     * the stable identifier assigned by the institution.</p>
     *
     * @return the person-id
     */
    @Override
    public String getId() {
        return pid;
    }

}
