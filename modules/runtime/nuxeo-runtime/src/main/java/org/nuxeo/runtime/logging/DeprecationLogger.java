/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.logging;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.api.Framework;

/**
 * Logger for deprecation that can hold the version from which this deprecation starts.
 * <p>
 * If the dev mode is set to {@code true} then logs the message as warning, otherwise logs as info if this level is
 * enabled, for more details see <a href="https://doc.nuxeo.com/nxdoc/logs-analysis/">documentation</a>
 *
 * @since 7.4
 */
public class DeprecationLogger {

    private static final Logger log = LogManager.getLogger(DeprecationLogger.class);

    public static void log(String message, String deprecatedVersion) {
        StringBuilder finalMessage = new StringBuilder();
        if (StringUtils.isNotBlank(deprecatedVersion)) {
            finalMessage.append("Since version ").append(deprecatedVersion).append(": ");
        }
        finalMessage.append(message);
        log(finalMessage.toString());
    }

    public static void log(String message) {
        if (Framework.isDevModeSet() || Framework.isTestModeSet()) {
            log.warn(message);
        } else {
            log.info(message);
        }
    }

}
