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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.api;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * @since 7.2
 */
public class AutomationScriptingException extends ClientException {

    private static final long serialVersionUID = 1L;

    public AutomationScriptingException() {
    }

    public AutomationScriptingException(String message) {
        super(message);
    }

    public AutomationScriptingException(String message, ClientException cause) {
        super(message, cause);
    }

    public AutomationScriptingException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutomationScriptingException(Throwable cause) {
        super(cause);
    }

    public AutomationScriptingException(ClientException cause) {
        super(cause);
    }

}
