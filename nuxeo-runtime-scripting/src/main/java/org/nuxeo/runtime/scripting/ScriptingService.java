/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.scripting;

import java.io.File;
import java.io.IOException;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ScriptingService {

    void setScriptDir(File scriptDir);

    File getScriptDir();

    File getScriptFile(String path);

    void registerScript(ScriptDescriptor sd);

    void unregisterScript(ScriptDescriptor sd);

    void unregisterScript(String name);

    boolean isScriptRegistered(String name);

    CompiledScript getScript(String name) throws ScriptException, IOException;

    CompiledScript compile(String path) throws ScriptException;

    Object eval(String path) throws ScriptException;

    Object eval(String path, ScriptContext ctx) throws ScriptException;

    ScriptEngine getEngineByFileName(String path);

    ScriptEngineManager getScriptEngineManager();

}
