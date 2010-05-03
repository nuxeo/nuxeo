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
package org.nuxeo.ecm.automation.core.scripting;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mvel2.MVEL;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Scripting {

    protected static Map<String,Script> cache = new ConcurrentHashMap<String, Script>();

    public static Expression newExpression(String expr) {
        return new MvelExpression(expr);
    }

    public static Expression newTemplate(String expr) {
        return new MvelTemplate(expr);
    }

    public static void run(OperationContext ctx, URL script) throws Exception {
        String key = script.toExternalForm();
        Script cs = cache.get(key);
        if (cs != null) {
            cs.eval(ctx);
            return;
        }
        String path = script.getPath();
        int p = path.lastIndexOf('.');
        if (p == -1) {
            throw new OperationException("Script files must have an extension: "+script);
        }
        String ext = path.substring(p+1).toLowerCase();
        if ("mvel".equals(ext)) {
            InputStream in = script.openStream();
            try {
                Serializable c = MVEL.compileExpression(FileUtils.read(in));
                cs = new MvelScript(c);
            } finally {
                in.close();
            }
        } else if ("groovy".equals(ext)) {
            //Script gs = new GroovyScript();
        } else {
            throw new OperationException("Unsupported script file: "+script+". Only mvel and groovy scripts are supported");
        }
        cache.put(key, cs);
        cs.eval(ctx);
    }

    public static Map<String,Object> initBindings(OperationContext ctx) {
        Object input = ctx.getInput(); // get last output
        HashMap<String, Object> map = new HashMap<String, Object>(ctx);
        map.put("CurrentDate", new DateWrapper());
        map.put("Context", ctx);
        map.put("This", input);
        map.put("Session", ctx.getCoreSession());
        map.put("Principal", ctx.getPrincipal());
        if (input instanceof DocumentModel) {
            map.put("Document", new DocumentWrapper(ctx.getCoreSession(), (DocumentModel)input));
        }
        return map;
    }

    public static interface Script {
        //protected long lastModified;
        void eval(OperationContext ctx) throws Exception;
    }

    public static class MvelScript implements Script {
        Serializable c;
        public MvelScript(Serializable c) {
            this.c = c;
        }
        public void eval(OperationContext ctx) throws Exception {
            MVEL.executeExpression(c, Scripting.initBindings(ctx));
        }
    }

    public static class GroovyScript implements Script {
        Serializable c;
        public GroovyScript(Serializable c) {
            this.c = c;
        }
        public void eval(OperationContext ctx) throws Exception {

        }
    }

}
