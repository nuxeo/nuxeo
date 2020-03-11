/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.script;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class Script {

    protected static volatile ScriptEngineManager scripting;

    protected CompiledScript script;

    public volatile long lastModified = -1;

    public static ScriptEngineManager getScripting() {
        if (scripting == null) {
            synchronized (Script.class) {
                if (scripting == null) {
                    scripting = new ScriptEngineManager();
                }
            }
        }
        return scripting;
    }

    public static Script newScript(String location) throws IOException {
        if (location.indexOf(':') > -1) {
            return newScript(new URL(location));
        } else {
            return new FileScript(location);
        }
    }

    public static Script newScript(URL location) throws IOException {
        String proto = location.getProtocol();
        if (proto.equals("jar")) {
            String path = location.getPath();
            int p = path.indexOf('!');
            if (p == -1) { // invalid jar URL .. returns a generic URL script
                return new URLScript(location);
            }
            path = path.substring(0, p);
            if (path.startsWith("file:")) {
                URI uri;
                try {
                    uri = new URI(path);
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
                return new JARFileScript(new File(uri), location);
            } else { // TODO import query string too?
                return new JARUrlScript(new URL(path), location);
            }
        } else if (proto.equals("file")) {
            URI uri;
            try {
                uri = location.toURI();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            return new FileScript(new File(uri));
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

    public Object run(Bindings args) throws ScriptException {
        if (args == null) {
            args = new SimpleBindings();
        }
        ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(args, ScriptContext.ENGINE_SCOPE);
        return getCompiledScript().eval(ctx);
    }

    public CompiledScript getCompiledScript() throws ScriptException {
        try {
            try (Reader reader = script == null ? getReader() : getReaderIfModified()) {
                if (reader != null) {
                    script = compile(reader);
                }
                return script;
            }
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

}
