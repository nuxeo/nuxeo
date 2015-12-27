/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
