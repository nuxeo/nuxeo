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

import java.io.InputStream;

import javax.script.ScriptException;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @since 7.2
 */
public interface AutomationScriptingService {

    /**
     * @return The default JS binding wrapper injected into Nashorn.
     */
    String getJSWrapper() throws OperationException;

    /**
     * Run Automation Scripting with given 'JavaScript' InputStream and CoreSession.
     * @param in
     * @param session
     * @throws ScriptException
     */
    void run(InputStream in, CoreSession session) throws ScriptException, OperationException;

    /**
     * Run Automation Scripting for a given 'JavaScript' script and CoreSession.
     * @param script
     * @param session
     * @throws ScriptException
     */
    void run(String script, CoreSession session) throws ScriptException, OperationException;

    /**
     * @param scriptingOperationInterface
     * @param script
     * @param session
     * @param <T>
     * @return
     * @throws ScriptException
     */
    <T> T getInterface(Class<T> scriptingOperationInterface, String script,
            CoreSession session) throws ScriptException, OperationException;

    /**
     * Activate/Deactivate Nashorn class filter.
     * @param activated
     */
    void setClassFilterActivation(boolean activated);

    /**
     * Activate/Deactivate Nashorn cache.
     * @param activated
     */
    void setCacheActivation(boolean activated);
}
