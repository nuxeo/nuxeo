/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile;

/**
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
public class UserProfileConstants {

    private UserProfileConstants() {
        // Constants class
    }

    public static final String USER_PROFILE_FACET = "UserProfile";

    public static final String USER_PROFILE_DOCTYPE = "UserProfile";

    public static final String USER_PROFILE_BIRTHDATE_FIELD = "userprofile:birthdate";

    public static final String USER_PROFILE_AVATAR_FIELD = "userprofile:avatar";

    public static final String USER_PROFILE_PHONENUMBER_FIELD = "userprofile:phonenumber";

    public static final String USER_PROFILE_GENDER_FIELD = "userprofile:gender";

    /**
     * @since 5.6
     */
    public static final String USER_PROFILE_TIMEZONE = "userprofile:timezone";

    /**
     * @since 5.6
     */
    public static final String USER_PROFILE_LOCALE = "userprofile:locale";
}
