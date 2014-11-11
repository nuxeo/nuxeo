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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemoteScript {

    private final ScriptingClient client;
    private final String name;
    private final String content;
    private ScriptContext ctx;

    RemoteScript(ScriptingClient client, String name, String content) {
        this.client = client;
        this.content = content;
        this.name = name;
        ctx = client.getScriptContext();
    }

    public Object eval() throws ScriptException {
        Map<String, Object> context = null;
        Writer out = null;
        if (ctx != null) {
            context = new HashMap<String, Object>(ctx.getBindings(ScriptContext.ENGINE_SCOPE));
            out = ctx.getWriter();
        }
        Object[] response;
        try {
            response = client.getServer().eval(name, content, context);
            String output = (String) response[0];
            if (out != null) {
                out.write(output);
                out.flush();
            } else {
                System.out.println(output);
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        }
        return response[1];
    }

    public Object invoke(String method, Object ... args) throws ScriptException {
        Map<String, Object> context = null;
        Writer out = null;
        if (ctx != null) {
            context = new HashMap<String, Object>(ctx.getBindings(ScriptContext.ENGINE_SCOPE));
            out = ctx.getWriter();
        }
        Object[] response;
        try {
            response = client.getServer().invoke(name, content, context, method, args);
            String output = (String) response[0];
            if (out != null) {
                out.write(output);
                out.flush();
            } else {
                System.out.println(output);
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        }
        return response[1];
    }

    public ScriptingClient getClient() {
        return client;
    }

    public void setContext(ScriptContext ctx) {
        this.ctx = ctx;
    }

    public ScriptContext getContext() {
        return ctx;
    }

}
