/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
