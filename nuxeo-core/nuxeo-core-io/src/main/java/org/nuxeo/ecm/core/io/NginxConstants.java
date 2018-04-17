/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.io;

/**
 * @since 10.3
 */
public final class NginxConstants {

    /** nuxeo.conf parameter to enable X-Accel in Nuxeo Platform. */
    public static final String X_ACCEL_ENABLED = "nuxeo.nginx.accel.enabled";

    public static final String X_ACCEL_LOCATION_HEADER = "X-Accel-Location";

    public static final String X_ACCEL_REDIRECT_HEADER = "X-Accel-Redirect";

    public static final String X_REQUEST_BODY_FILE_HEADER = "X-Request-Body-File";

    public static final String X_CONTENT_MD5_HEADER = "X-Content-MD5";

    private NginxConstants() {
        // instantiation disallowed
    }
}
