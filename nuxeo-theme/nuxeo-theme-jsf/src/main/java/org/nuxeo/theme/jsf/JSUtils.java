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

package org.nuxeo.theme.jsf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Main;

public final class JSUtils {

    private static final Log log = LogFactory.getLog(JSUtils.class);

    final private static Global global = Main.getGlobal();

    static {
        ToolErrorReporter errorReporter = new ToolErrorReporter(false,
                global.getErr());
        Main.shellContextFactory.setErrorReporter(errorReporter);
        global.init(Main.shellContextFactory);
    }

    public static String compressSource(final String source) {
        IProxy iproxy = new IProxy(source);
        return (String) Main.shellContextFactory.call(iproxy);
    }

    private static class IProxy implements ContextAction {
        final private String source;

        IProxy(String source) {
            this.source = source;
        }

        public Object run(final Context cx) {
            try {
                final Script script = Main.loadScriptFromSource(cx, source,
                        "compress", 1, null);
                return cx.compressReader(global, script, source, "compress", 1,
                        null);
            } catch (IllegalArgumentException e) {
                // Can happen on very large files (> 500K) with JDK 5
                log.error("Could not compress javascript source.");
                return source;
            }
        }
    }

}
