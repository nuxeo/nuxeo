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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.scripting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.webengine.DefaultWebContext;
import org.nuxeo.ecm.webengine.WebApplication;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.scripting.ScriptingService;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyTuple;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Scripting {

    private static final String CHAR_FILE_EXT = "html htm xml css txt java c cpp h";
    private static final String BINARY_FILE_EXT = "gif jpg jpeg png pdf doc xsl";

    private final ConcurrentMap<File, Entry> cache = new ConcurrentHashMap<File, Entry>();

    final RenderingEngine renderingEngine;
    final ScriptingService  scriptService;

    public Scripting(RenderingEngine engine) {
        renderingEngine = engine;
        scriptService = Framework.getLocalService(ScriptingService.class);
        if (scriptService == null) {
            throw new RuntimeException("Scripting is not enabled: Put nuxeo-runtime-scripting in the classpath");
        }
    }

    public RenderingEngine getRenderingEngine() {
        return renderingEngine;
    }

    public void exec(WebContext context, ScriptFile script) throws Exception {
        exec(context, script, null);
    }

    public void exec(WebContext context, ScriptFile script, Map<String, Object> args) throws Exception {
        String ext = script.getExtension();
        if ("ftl".equals(ext)) {
            context.render(script.getFile().getAbsolutePath(), args); //TODO
        } else {
            runScript(context, script, args);
        }
    }


    public void exec(WebContext context) throws Exception {
        ScriptFile script = context.getTargetScript();
        if (script.getFile().isFile()) {
            exec(context, script);
        } else {
            WebApplication app = context.getApplication();
            script = app.getScript(app.getDefaultPage());
            exec(context, script);
        }
    }

    public static CompiledScript compileScript(ScriptEngine engine, File file) throws ScriptException {
        if (engine instanceof Compilable) {
            Compilable comp = (Compilable)engine;
            try {
                Reader reader = new FileReader(file);
                try {
                    return comp.compile(reader);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } else {
            return null;
        }
    }

    public void runScript(WebContext context, ScriptFile script) throws Exception {
        runScript(context, script, null);
    }

    public void runScript(WebContext context, ScriptFile script, Map<String, Object> args) throws Exception {
        // script is not compilable - run slow eval
        String ext = script.getExtension();
        ScriptEngine engine = scriptService.getScriptEngineManager().getEngineByExtension(ext);
        if (engine != null) {
            Bindings bindings = ((DefaultWebContext)context).createBindings(args); //TODO put method in interface?
            ScriptContext ctx = new SimpleScriptContext();
            ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            CompiledScript comp = getScript(engine, script.getFile()); // use cache for compiled scripts
            if (comp != null) {
                comp.eval(ctx);
                return;
            } // compilation not supported - eval it on the fly
            try {
                Reader reader = new FileReader(script.getFile());
                try {
                    engine.eval(reader, ctx);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } else {
            if (CHAR_FILE_EXT.contains(ext)) { //TODO use char writer instead of stream
                FileInputStream in = new FileInputStream(script.getFile());
                try {
                    FileUtils.copy(in, context.getResponse().getOutputStream());
                } finally {
                    in.close();
                }
            } else if (BINARY_FILE_EXT.contains(ext)) {
                FileInputStream in = new FileInputStream(script.getFile());
                try {
                    FileUtils.copy(in, context.getResponse().getOutputStream());
                } finally {
                    in.close();
                }
            } else {
                throw new ScriptException(
                        "No script engine was found for the file: " + script.getFile().getName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Map convertPythonMap(PyDictionary dict) {
        PyList list = dict.items();
        Map<String, PyObject> table = new HashMap();
        for(int i = list.__len__(); i-- >  0; ) {
            PyTuple tup = (PyTuple) list.__getitem__(i);
            String key = tup.__getitem__(0).toString();
            table.put(key, tup.__getitem__(1));
        }
        return table;
    }

    public CompiledScript getScript(ScriptEngine engine, File file) throws ScriptException {
        Entry entry = cache.get(file);
        long tm = file.lastModified();
        if (entry != null) {
            if (entry.lastModified < tm) { // recompile
                entry.script = compileScript(engine, file);
                entry.lastModified = tm;
            }
            return entry.script;
        }
        CompiledScript script = compileScript(engine, file);
        if (script != null) {
            cache.putIfAbsent(file, new Entry(script, tm));
            return script;
        }
        return null;
    }

    class Entry {
        public CompiledScript script;
        public long lastModified;

        Entry(CompiledScript script, long lastModified) {
            this.lastModified = lastModified;
            this.script = script;
        }
    }
}
