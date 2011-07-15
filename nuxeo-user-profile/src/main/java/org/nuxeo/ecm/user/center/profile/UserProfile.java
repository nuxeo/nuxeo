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

import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.userpreferences.UserPreferences;

/**
 * An object to store additional user information
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.4.3
 */
public interface UserProfile extends UserPreferences<UserProfile> {

    Calendar getBirthDate() throws ClientException;

    Blob getAvatar() throws ClientException;

    String getPhoneNumber() throws ClientException;

    /**
     * @return {@code Boolean.FALSE} for Man or {@code Boolean.TRUE} for Woman
     * @throws ClientException
     */
    Boolean getGender() throws ClientException;

    String getSchoolWorkInfo() throws ClientException;

    String getInterests() throws ClientException;

    String getCareerObjective() throws ClientException;

    String getSkills() throws ClientException;

    Boolean getPublicProfile() throws ClientException;

}
