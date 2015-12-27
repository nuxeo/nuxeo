/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.logging;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Logger for deprecation warnings.
 * <p>
 * Can be activated only when dev mode is set, and can hold the version from which deprecation starts.
 *
 * @since 7.4
 */
public class DeprecationLogger {

    private static final Log log = LogFactory.getLog(DeprecationLogger.class);

    public static final void log(String message, String deprecatedVersion) {
        StringBuilder finalMessage = new StringBuilder();
        if (!StringUtils.isBlank(deprecatedVersion)) {
            finalMessage.append("Since version ").append(deprecatedVersion).append(": ");
        }
        finalMessage.append(message);
        log(finalMessage.toString());
    }

    public static final void log(String message) {
        if (Framework.isDevModeSet()) {
            log.warn(message);
        }
    }

}
