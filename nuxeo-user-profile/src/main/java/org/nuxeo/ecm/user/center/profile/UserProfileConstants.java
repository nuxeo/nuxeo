/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    public static final String USER_PROFILE_TIMEZONE = "userprofile:timezone";

    public static final String USER_PROFILE_LOCALE = "userprofile:locale";
}
