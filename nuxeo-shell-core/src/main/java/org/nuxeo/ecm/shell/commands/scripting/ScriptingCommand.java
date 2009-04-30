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

package org.nuxeo.ecm.shell.commands.scripting;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.repository.AbstractCommand;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptingCommand extends AbstractCommand {

    protected static final ScriptEngineManager scriptMgr = new ScriptEngineManager();

    protected CompiledScript script;

    public ScriptingCommand(File file) throws ScriptException {
        script = compileScript(file);
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        Bindings ctx = new SimpleBindings();
        ctx.put("cmdLine", cmdLine);
        ctx.put("ctx", context);
        ctx.put("client", client);
        ctx.put("service", cmdService);
        try {
            script.eval(ctx);
        } finally {
            System.out.flush();
        }
    }

    public static ScriptEngine getEngineByExtension(String extension) {
        return scriptMgr.getEngineByExtension(extension);
    }

    public static CompiledScript compileScript(File file) throws ScriptException {
        String ext = FileUtils.getFileExtension(file.getName());
        ScriptEngine engine = scriptMgr.getEngineByExtension(ext);
        return compileScript(engine, file);
    }

    public static CompiledScript compileScript(ScriptEngine engine, File file) throws ScriptException {
        if (engine instanceof Compilable) {
            Compilable comp = (Compilable)engine;
            try {
                Reader reader = new FileReader(file);
                try {
                    return comp.compile(reader);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } else {
            return null;
        }
    }

}
