/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.script;

import java.io.Reader;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Simulates a compiled script for scripts that don't support
 * compilation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FakeCompiledScript extends CompiledScript {

    protected final ScriptEngine engine;
    protected final Script script;

    public FakeCompiledScript(ScriptEngine engine, Script script) {
        this.script = script;
        this.engine = engine;
    }

    @Override
    public Object eval(ScriptContext arg0) throws ScriptException {
        try {
            Reader reader = script.getReader();
            return engine.eval(reader, arg0);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }

}
