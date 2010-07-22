/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.html;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Main;

public final class JSUtils {

    static final Log log = LogFactory.getLog(JSUtils.class);

    private static final Global global = Main.getGlobal();
    private static Method compress;

    static {
        ToolErrorReporter errorReporter = new ToolErrorReporter(false,
                global.getErr());
        Main.shellContextFactory.setErrorReporter(errorReporter);
        global.init(Main.shellContextFactory);
        try {
            compress = Context.class.getMethod("compressReader", Context.class, Scriptable.class, Script.class, String.class, String.class, Integer.TYPE, Object.class);
        } catch (NoSuchMethodException e) {
            log.warn("No javascript compressor support available, check for your rhino engine.");
        }
   }

    public static String compressSource(final String source) {
        IProxy iproxy = new IProxy(source);
        return (String) Main.shellContextFactory.call(iproxy);
    }

    private static class IProxy implements ContextAction {
        private final String source;

        IProxy(String source) {
            this.source = source;
        }

        public Object run(final Context cx) {
            if (compress == null) {
                return source;
            }
            try {
                Script script = Main.loadScriptFromSource(cx, source, "compress", 1, null);
                    return compress.invoke(cx, global, script, source, "compress", 1, null);
            } catch (Exception e) {
                // Can happen on very large files (> 500K) with JDK 5
                log.error("Could not compress javascript source.", e);
                return source;
            }
        }
    }

}
