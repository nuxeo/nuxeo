/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.ecm.automation.core.scripting;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.mvel2.MVEL;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.context.ContextService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;

import groovy.lang.Binding;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Scripting {

    protected static final Map<String, Script> cache = new ConcurrentHashMap<>();

    protected static final GroovyScripting gscripting = new GroovyScripting();

    public static Expression newExpression(String expr) {
        return new MvelExpression(expr);
    }

    public static Expression newTemplate(String expr) {
        return new MvelTemplate(expr);
    }

    public static void run(OperationContext ctx, URL script) throws OperationException, IOException {
        String key = script.toExternalForm();
        Script cs = cache.get(key);
        if (cs != null) {
            cs.eval(ctx);
            return;
        }
        String path = script.getPath();
        int p = path.lastIndexOf('.');
        if (p == -1) {
            throw new OperationException("Script files must have an extension: " + script);
        }
        String ext = path.substring(p + 1).toLowerCase();
        try (InputStream in = script.openStream()) {
            if ("mvel".equals(ext)) {
                Serializable c = MVEL.compileExpression(IOUtils.toString(in, UTF_8));
                cs = new MvelScript(c);
            } else if ("groovy".equals(ext)) {
                cs = new GroovyScript(IOUtils.toString(in, UTF_8));
            } else {
                throw new OperationException(
                        "Unsupported script file: " + script + ". Only MVEL and Groovy scripts are supported");
            }
            cache.put(key, cs);
            cs.eval(ctx);
        }
    }

    public static Map<String, Object> initBindings(OperationContext ctx) {
        Object input = ctx.getInput(); // get last output
        Map<String, Object> map = new HashMap<>(ctx.getVars());
        map.put("CurrentDate", new DateWrapper());
        map.put("Context", ctx);
        if (ctx.get(Constants.VAR_WORKFLOW) != null) {
            map.put(Constants.VAR_WORKFLOW, ctx.get(Constants.VAR_WORKFLOW));
        }
        if (ctx.get(Constants.VAR_WORKFLOW_NODE) != null) {
            map.put(Constants.VAR_WORKFLOW_NODE, ctx.get(Constants.VAR_WORKFLOW_NODE));
        }
        map.put("This", input);
        map.put("Session", ctx.getCoreSession());
        PrincipalWrapper principalWrapper = new PrincipalWrapper(ctx.getPrincipal());
        map.put("CurrentUser", principalWrapper);
        // Alias
        map.put("currentUser", principalWrapper);
        map.put("Env", Framework.getProperties());

        // Helpers injection
        ContextService contextService = Framework.getService(ContextService.class);
        map.putAll(contextService.getHelperFunctions());

        if (input instanceof DocumentModel) {
            DocumentWrapper documentWrapper = new DocumentWrapper(ctx.getCoreSession(), (DocumentModel) input);
            map.put("Document", documentWrapper);
            // Alias
            map.put("currentDocument", documentWrapper);
        }
        if (input instanceof DocumentModelList) {
            List<DocumentWrapper> docs = new ArrayList<>();
            for (DocumentModel doc : (DocumentModelList) input) {
                docs.add(new DocumentWrapper(ctx.getCoreSession(), doc));
            }
            map.put("Documents", docs);
            if (!docs.isEmpty()) {
                map.put("Document", docs.get(0));
            }
        }
        return map;
    }

    public interface Script {
        Object eval(OperationContext ctx);
    }

    public static class MvelScript implements Script {
        final Serializable c;

        public static MvelScript compile(String script) {
            return new MvelScript(MVEL.compileExpression(script));
        }

        public MvelScript(Serializable c) {
            this.c = c;
        }

        @Override
        public Object eval(OperationContext ctx) {
            return MVEL.executeExpression(c, Scripting.initBindings(ctx));
        }
    }

    public static class GroovyScript implements Script {
        final groovy.lang.Script c;

        public GroovyScript(String c) {
            this.c = gscripting.getScript(c, new Binding());
        }

        @Override
        public Object eval(OperationContext ctx) {
            Binding binding = new Binding();
            for (Map.Entry<String, Object> entry : initBindings(ctx).entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            binding.setVariable("out", new PrintStream(baos));
            c.setBinding(binding);
            c.run();
            return baos;
        }
    }

}
