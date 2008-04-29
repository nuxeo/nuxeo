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

package org.nuxeo.ecm.platform.site.security.guards;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.security.Guard;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.scripting.ScriptingService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("script")
public class ScriptGuard implements Guard {

    @XContent
    protected String script;
    @XNode("@type")
    protected String type;
    @XNode("@src")
    protected String src;

    protected ScriptEngine engine;
    protected CompiledScript comp;

    protected ScriptGuard() {
    }

    public ScriptGuard(String type, String script) {
        this.type = type;
        this.script = script;
    }

    public boolean check(CoreSession session, DocumentModel doc) {
        try {
            if (engine == null) {
                comp = compile(type, script);
            }
            Bindings bindings = new SimpleBindings();
            bindings.put("doc", doc);
            bindings.put("session", session);
            Boolean result;
            if (comp != null) {
                result = (Boolean)comp.eval(bindings);
            } else {
                result = (Boolean)engine.eval(new StringReader(script), bindings);
            }
            return result != null ? result.booleanValue() : false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String toString() {
        return "SCRIPT:"+type+"["+script+"]";
    }

    private CompiledScript compile(String type, String content) throws ScriptException {
        engine = Framework.getLocalService(ScriptingService.class)
            .getScriptEngineManager().getEngineByName(type);
        if (engine != null) {
             if (engine instanceof Compilable) {
                 try {
                     Reader reader = new StringReader(content);
                     try {
                         return ((Compilable) engine).compile(reader);
                     } finally {
                         reader.close();
                     }
                 } catch (IOException e) {
                     throw new ScriptException(e);
                 }
             } else {
                 return null; // script is not compilable
             }
        } else {
            throw new ScriptException(
                    "No suitable script engine found for the file " + type);
        }
    }

}
