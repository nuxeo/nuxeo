/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth;

public final class ShibbolethConstants {
    private ShibbolethConstants() {
        // Constants class
    }

    public static final String USER_SHIBBOLETH_GROUP_HEADER = "headers";

    public static final String SHIBBOLETH_DIRECTORY = "shibbGroup";

    public static final String SHIBBOLETH_SCHEMA = "shibbolethGroup";

    public static final String SHIBBOLETH_DOCTYPE = "shibbGroup";

    public static final String GROUP_EL_PROPERTY = "expressionLanguage";

    public static final String EL_CURRENT_USER_NAME = "currentUser";

    public static final String GROUP_ID_PROPERTY = "groupName";
}
