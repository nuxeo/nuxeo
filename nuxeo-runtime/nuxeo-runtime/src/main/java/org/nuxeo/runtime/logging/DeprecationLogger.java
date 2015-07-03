/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
