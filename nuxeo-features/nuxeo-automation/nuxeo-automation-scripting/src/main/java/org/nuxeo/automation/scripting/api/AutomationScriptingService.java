/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.api;

import javax.script.ScriptException;

import org.nuxeo.automation.scripting.internals.ScriptRunner;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @since 7.2
 */
public interface AutomationScriptingService {

    /**
     * @return The default JS binding wrapper injected into Nashorn.
     */
    String getJSWrapper();

    /**
     * Get the script runner for a given core session.
     *
     * @param session the core session
     * @return the runner
     * @throws ScriptException
     */
    ScriptRunner getRunner(CoreSession session) throws ScriptException;

}
