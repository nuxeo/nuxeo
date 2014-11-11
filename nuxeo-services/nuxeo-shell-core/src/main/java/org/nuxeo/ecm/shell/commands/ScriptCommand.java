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

package org.nuxeo.ecm.shell.commands;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.scripting.ScriptingService;

/** @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> */
public class ScriptCommand implements Command {
    private static final Log log = LogFactory.getLog(ScriptCommand.class);

    public void printHelp(PrintStream out) {
        // TODO Auto-generated method stub
    }

    public void run(CommandLine cmdLine) throws Exception {

        String file = cmdLine.getOption("file");
        if (file == null) {
            // TODO: support for std input stream not yet impl
            log.error("STDIN support not yet implemented. Neither stdin supported, neither any file given as input => aborting ... sorry :)");
            System.exit(10);
        }

        ScriptContext ctx = new SimpleScriptContext();
        ctx.setAttribute("cmdLine", cmdLine, ScriptContext.ENGINE_SCOPE);
        eval(file, ctx);
    }

    public Object eval(String path, ScriptContext ctx) throws ScriptException {
        ScriptingService ss = null;

        // check to be sure scripting support is in classpath
        try {
            ss = Framework.getLocalService(ScriptingService.class);
        } catch (Exception e) {
            log.error("Scripting is not enabled. To enable it copy the nuxeo-runtime-scripting bundle in app/bundles");
            System.exit(10);
        }

        ScriptEngine engine = ss.getEngineByFileName(path);
        if (engine != null) {
            try {
                Reader reader = new FileReader(new File(path));
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

}
