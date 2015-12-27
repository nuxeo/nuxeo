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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.api;

import java.io.InputStream;

import javax.script.ScriptException;

import org.nuxeo.automation.scripting.internals.ScriptOperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @since 7.2
 */
public interface AutomationScriptingService {

    /**
     * @since 7.3
     */
    void setOperationContext(ScriptOperationContext ctx);

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

}
