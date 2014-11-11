/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.scripting;

import groovy.lang.Binding;

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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Scripting {

    protected static final Map<String, Script> cache = new ConcurrentHashMap<String, Script>();

    protected static final GroovyScripting gscripting = new GroovyScripting();

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
            throw new OperationException(
                    "Script files must have an extension: " + script);
        }
        String ext = path.substring(p + 1).toLowerCase();
        if ("mvel".equals(ext)) {
            InputStream in = script.openStream();
            try {
                Serializable c = MVEL.compileExpression(FileUtils.read(in));
                cs = new MvelScript(c);
            } finally {
                in.close();
            }
        } else if ("groovy".equals(ext)) {
            // Script gs = new GroovyScript();
        } else {
            throw new OperationException("Unsupported script file: " + script
                    + ". Only mvel and groovy scripts are supported");
        }
        cache.put(key, cs);
        cs.eval(ctx);
    }

    public static Map<String, Object> initBindings(OperationContext ctx) {
        Object input = ctx.getInput(); // get last output
        Map<String, Object> map = new HashMap<String, Object>(ctx);
        map.put("CurrentDate", new DateWrapper());
        map.put("Context", ctx);
        map.put("This", input);
        map.put("Session", ctx.getCoreSession());
        map.put("CurrentUser",
                new PrincipalWrapper((NuxeoPrincipal) ctx.getPrincipal()));
        map.put("Env", Framework.getProperties());
        map.put("Fn", Functions.getInstance());
        if (input instanceof DocumentModel) {
            map.put("Document", new DocumentWrapper(ctx.getCoreSession(),
                    (DocumentModel) input));
        }
        return map;
    }

    public interface Script {
        // protected long lastModified;
        Object eval(OperationContext ctx) throws Exception;
    }

    public static class MvelScript implements Script {
        final Serializable c;

        public static MvelScript compile(String script) {
            return new MvelScript(MVEL.compileExpression(script));
        }

        public MvelScript(Serializable c) {
            this.c = c;
        }

        public Object eval(OperationContext ctx) throws Exception {
            return MVEL.executeExpression(c, Scripting.initBindings(ctx));
        }
    }

    public static class GroovyScript implements Script {
        final groovy.lang.Script c;

        public GroovyScript(String c) {
            this.c = gscripting.getScript(c, new Binding());
        }

        public Object eval(OperationContext ctx) throws Exception {
            Binding binding = new Binding();
            for (Map.Entry<String, Object> entry : initBindings(ctx).entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }
            c.setBinding(binding);
            return c.run();
        }
    }

}
