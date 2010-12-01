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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptingComponent extends DefaultComponent implements ScriptingService {

    private static final Log log = LogFactory.getLog(ScriptingService.class);

    private ScriptEngineManager scriptMgr;
    private Map<String, ScriptDescriptor> scripts;
    private File scriptDir;
    private ScriptingServer server;


    @Override
    public void activate(ComponentContext context) throws Exception {
        RuntimeService runtime = Framework.getRuntime();
        String scrPath = Framework.getRuntime().getProperty("org.nuxeo.scripts.dir");
        if (scrPath == null) {
            //Bundle bundle = context.getRuntimeContext().getBundle();
            //new File(bundle.getLocation());
            scriptDir = new File(runtime.getHome(), "scripts");
        } else {
            scriptDir = new File(scrPath);
        }
        scripts = new Hashtable<String, ScriptDescriptor>();
        scriptMgr = new ScriptEngineManager();

        // start remote scripting service
        Boolean isServer = (Boolean) context.getPropertyValue("isServer", Boolean.TRUE);
//TODO: server functionality should be removed
//        if (isServer) {
//            server = new ScriptingServerImpl(this);
//            RemotingService remoting = (RemotingService) Framework.getRuntime().getComponent(RemotingService.NAME);
//            TransporterServer transporterServer = remoting.getTransporterServer();
//            transporterServer.addHandler(server, ScriptingServer.class.getName());
//        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
//        if (server != null) {
//            RemotingService remoting = (RemotingService)Framework.getRuntime().getComponent(RemotingService.NAME);
//            TransporterServer transporterServer = remoting.getTransporterServer();
//            transporterServer.removeHandler();
//        }
        server = null;
        scriptMgr = null;
        scripts = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        ScriptDescriptor sd = (ScriptDescriptor) contribution;
        sd.ctx = contributor.getRuntimeContext();
        registerScript(sd);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        ScriptDescriptor sd = (ScriptDescriptor) contribution;
        unregisterScript(sd);
    }

    @Override
    public ScriptEngineManager getScriptEngineManager() {
        return scriptMgr;
    }

    @Override
    public void setScriptDir(File scriptDir) {
        this.scriptDir = scriptDir;
    }

    @Override
    public File getScriptDir() {
        return scriptDir;
    }

    @Override
    public File getScriptFile(String path) {
        return new File(scriptDir, path);
    }

    @Override
    public void registerScript(ScriptDescriptor sd) {
        if (sd.name == null) {
            sd.name = sd.src;
        }
        scripts.put(sd.name, sd);
    }

    @Override
    public void unregisterScript(ScriptDescriptor sd) {
        if (sd.name == null) {
            sd.name = sd.src;
        }
        scripts.remove(sd.name);
    }

    @Override
    public void unregisterScript(String name) {
        scripts.remove(name);
    }

    @Override
    public boolean isScriptRegistered(String name) {
        return scripts.containsKey(name);
    }

    @Override
    public CompiledScript getScript(String name) throws ScriptException, IOException {
        ScriptDescriptor sd = scripts.get(name);
        if (sd != null) {
            if (sd.script != null) {
                return sd.script;
            }
            ScriptEngine engine = getEngineByFileName(sd.src);
            if (engine == null) {
                log.warn("Script engine not found for: " + sd.src);
            } else if (engine instanceof Compilable) {
                try {
                    Reader reader = getReader(sd);
                    try {
                        sd.script = ((Compilable) engine).compile(reader);
                        return sd.script;
                    } finally {
                        reader.close();
                    }
                } catch (ScriptException e) {
                    throw new ScriptException("Script file was not found: "
                            + sd.src + " when trying to load " + name);
                }
            } else {
                throw new ScriptException("Not a compilable scripting engine: "
                        + engine.getFactory().getEngineName());
            }
        }
        return null;
    }

    @Override
    public CompiledScript compile(String path) throws ScriptException {
        ScriptEngine engine = getEngineByFileName(path);
        if (engine != null) {
             if (engine instanceof Compilable) {
                 try {
                     Reader reader = new FileReader(getScriptFile(path));
                     try {
                         return ((Compilable) engine).compile(reader);
                     } finally {
                         reader.close();
                     }
                 } catch (IOException e) {
                     throw new ScriptException(e);
                 }
             } else {
                 throw new ScriptException("Script Engine "
                         + engine.getFactory().getEngineName() + " is not compilable");
             }
        } else {
            throw new ScriptException(
                    "No suitable script engine found for the file " + path);
        }
    }

    @Override
    public Object eval(String path) throws ScriptException {
        ScriptEngine engine = getEngineByFileName(path);
        if (engine != null) {
            try {
                Reader reader = new FileReader(getScriptFile(path));
                try {
                    return engine.eval(reader);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } else {
            throw new ScriptException(
                    "No script engine was found for the file: " + path);
        }
    }

    @Override
    public Object eval(String path, ScriptContext ctx) throws ScriptException {
        ScriptEngine engine = getEngineByFileName(path);
        if (engine != null) {
            try {
            Reader reader = new FileReader(getScriptFile(path));
                try {
                    return engine.eval(reader, ctx);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } else {
            throw new ScriptException(
                    "No script engine was found for the file: " + path);
        }
    }

    @Override
    public ScriptEngine getEngineByFileName(String path) {
        String ext = getFileExtension(path);
        return ext == null ? null : scriptMgr.getEngineByExtension(ext);
    }

    public static String getFileExtension(String path) {
        int p = path.lastIndexOf('.');
        if (p > -1) {
            return path.substring(p + 1);
        }
        return null;
    }

    private Reader getReader(ScriptDescriptor sd) throws IOException {
        Reader reader;
        if (sd.ctx == null) { //may be a file
            File file = new File(scriptDir, sd.src);
            reader = new FileReader(file);
        } else {
            URL url = sd.ctx.getLocalResource(sd.src);
            if (url != null) {
                reader = new InputStreamReader(url.openStream());
            } else {
                throw new FileNotFoundException(sd.src);
            }
        }
        return reader;
    }

}
