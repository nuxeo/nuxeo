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

package org.nuxeo.ecm.platform.site.template;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.scripting.ScriptingService;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Scripting {

    RenderingEngine renderingEngine;
    ScriptingService  scriptService;



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

    public void exec(SiteRequest req) throws Exception {
        ScriptFile script = req.getScript();
        if (script.getFile().isFile()) {
            String ext = script.getExtension();
            if ("ftl".equals(ext)) {
                renderingEngine.render(script.getPath(), req.getLastResolvedObject());
            } else {
                runScript(req, script);
            }
        } else {
            req.getResponse().getWriter().write("Could not find script: "+script.getPath()+". Error handling not yet implemented ");
        }
    }

    protected void runScript(SiteRequest req, ScriptFile script) throws Exception {
        ScriptEngine engine = scriptService.getScriptEngineManager().getEngineByExtension(script.getExtension());
        if (engine != null) {
            try {
                Reader reader = new FileReader(script.getFile());
                try {
                    ScriptContext ctx = new SimpleScriptContext();
                    ctx.setAttribute("req", req, ScriptContext.ENGINE_SCOPE);
                    ctx.setAttribute("scripting", this, ScriptContext.ENGINE_SCOPE);
                    ctx.setAttribute("out", req.getResponse().getWriter(), ScriptContext.ENGINE_SCOPE);
                    engine.eval(reader, ctx);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } else {
            throw new ScriptException(
                    "No script engine was found for the file: " + script.getPath());
        }
    }


}
