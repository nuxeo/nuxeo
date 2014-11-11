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

package org.nuxeo.ecm.webengine.security.guards;

import java.io.StringReader;
import java.security.Principal;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("script")
public class ScriptGuard implements Guard {

    private static final Log log = LogFactory.getLog(ScriptGuard.class);

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

    public boolean check(Adaptable context) {
        try {
            if (engine == null) {
                comp = compile(type, script);
            }
            Bindings bindings = new SimpleBindings();
            bindings.put("Context", context);
            bindings.put("doc", context.getAdapter(DocumentModel.class));
            bindings.put("session", context.getAdapter(CoreSession.class));
            bindings.put("principal", context.getAdapter(Principal.class));
            Object result = null;
            if (comp != null) {
                result = comp.eval(bindings);
                if (result == null) {
                    result = bindings.get("__result__");
                }
            } else {
                result = engine.eval(new StringReader(script), bindings);
            }
            return booleanValue(result);
        } catch (Exception e) {
            log.error(e, e);
            return false;
        }
    }

    protected static boolean booleanValue(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj.getClass() == Boolean.class) {
            return (Boolean) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue() != 0;
        }
        return false;
    }

    @Override
    public String toString() {
        return "SCRIPT:" + type + '[' + script + ']';
    }

    private CompiledScript compile(String type, String content) throws ScriptException {
        if (engine == null) {
            engine = Framework.getLocalService(WebEngine.class)
                    .getScripting().getEngineManager().getEngineByName(type);
        }
        if (engine != null) {
            if (engine instanceof Compilable) {
                return ((Compilable) engine).compile(content);
            } else {
                return null; // script is not compilable
            }
        } else {
            throw new ScriptException(
                    "No suitable script engine found for the file " + type);
        }
    }

}
