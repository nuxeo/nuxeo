/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.scripting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptingServerImpl implements ScriptingServer {

    private final ScriptingComponent comp;


    public ScriptingServerImpl(ScriptingComponent comp) {
        this.comp = comp;
    }

    @Override
    public Object[] eval(String name, String script, Map<String, Object> context)
            throws Exception {
        ScriptEngine engine = comp.getEngineByFileName(name);
        if (engine == null) {
            throw new Exception("No suitable script engine found for " + name);
        }
        engine.put(ScriptEngine.FILENAME, name);
        ScriptContext ctx = engine.getContext();
        if (context != null) {
            ctx.getBindings(ScriptContext.ENGINE_SCOPE).putAll(context);
        }
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        ctx.setWriter(out);
        ctx.setErrorWriter(out);
        Object[] response = new Object[2];
        response[1] = engine.eval(script, ctx);
        out.flush();
        response[0] = writer.getBuffer().toString();
        return response;
    }

    @Override
    public Object[] invoke(String name, String script,
            Map<String, Object> context, String method, Object[] args)
            throws Exception {
        ScriptEngine engine = comp.getEngineByFileName(name);
        if (engine == null) {
            throw new Exception("No suitable script engine found for " + name);
        }
        engine.put(ScriptEngine.FILENAME, name);

        ScriptContext ctx;
        if (context != null) {
            ctx = new SimpleScriptContext();
            ctx.getBindings(ScriptContext.ENGINE_SCOPE).putAll(context);
        } else {
            ctx = engine.getContext();
        }
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        ctx.setWriter(out);
        ctx.setErrorWriter(out);
        engine.eval(script, ctx);
        Object[] response = new Object[2];
        if (engine instanceof Invocable) {
            response[1] = ((Invocable) engine).invokeFunction(method, args);
        } else {
            throw new Exception("The script engine does not support invoking: "
                    + engine.getFactory().getEngineName());
        }
        out.flush();
        response[0] = writer.getBuffer().toString();
        return response;
    }

}
