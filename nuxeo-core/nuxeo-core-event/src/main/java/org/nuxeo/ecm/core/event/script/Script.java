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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class Script {

    public static boolean trackChanges = true;
    public static ScriptEngineManager scripting;

    public CompiledScript script;
    public long lastModified = -1;

    public static ScriptEngineManager getScripting() {
        if (scripting == null) {
            synchronized (Script.class) {
                scripting = new ScriptEngineManager();
            }
        }
        return scripting;
    }

    public static Script newScript(String location) throws Exception {
        if (location.indexOf(':') > -1) {
            return newScript(new URL(location));
        } else {
            return new FileScript(location);
        }
    }

    public static Script newScript(URL location) throws Exception {
        String proto = location.getProtocol();
        if (proto.equals("jar")) {
            String path = location.getPath();
            int p = path.indexOf('!');
            if (p == -1) { // invalid jar URL .. returns a generic URL script
                return new URLScript(location);
            }
            path = path.substring(0, p);
            if (path.startsWith("file:")) {
                return new JARFileScript(new File(new URI(path)), location);
            } else { // TODO import query string too?
                return new JARUrlScript(new URL(path), location);
            }
        } else if (proto.equals("file")) {
            return new FileScript(new File(location.toURI()));
        } else {
            return new URLScript(location);
        }
    }

    public static Script newScript(File location) {
        return new FileScript(location);
    }

    public abstract Reader getReader() throws IOException;

    public abstract Reader getReaderIfModified() throws IOException;

    public abstract String getExtension();

    public abstract String getLocation();

    protected String getExtension(String location) {
        int p = location.lastIndexOf('.');
        if (p > -1) {
            return location.substring(p + 1);
        }
        return null;
    }

    public Object run(Bindings args) throws Exception {
        if (args == null) {
            args = new SimpleBindings();
        }
        ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(args, ScriptContext.ENGINE_SCOPE);
        Object result = null;
        if (!trackChanges && script != null) {
            result = script.eval(ctx);
        } else {
            result = getCompiledScript().eval(ctx);
        }
        return result;
    }

    public CompiledScript getCompiledScript() throws ScriptException {
        try {
            Reader reader = script == null ? getReader() : getReaderIfModified(); 
            if (reader != null) { 
                script = compile(reader);
            }
            return script;
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    public CompiledScript compile(Reader reader) throws ScriptException {
        ScriptEngine engine = getScripting().getEngineByExtension(getExtension());
        if (engine == null) {
            throw new ScriptException("Unknown script type: " + getExtension());
        }
        if (engine instanceof Compilable) {
            Compilable comp = (Compilable) engine;
            try {
                try {
                    return comp.compile(reader);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } else {// TODO this will read sources twice the fist time - pass
                // reader?
            return new FakeCompiledScript(engine, this);
        }
    }

    // FIXME: make a proper test and remove.
    public static void main(String[] args) throws Exception {
        URL url = new URL(
                "jar:file:///Users/bstefanescu/work/kits/freemarker-2.3.15/lib/freemarker.jar!/freemarker/version.properties");
        System.out.println(">>" + url.getProtocol());
        System.out.println(">>" + url.getPath());
        System.out.println(">>" + FileUtils.read(url.openStream()));
    }

}
