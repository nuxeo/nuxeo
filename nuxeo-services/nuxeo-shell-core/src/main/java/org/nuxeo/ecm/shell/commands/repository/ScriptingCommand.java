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

package org.nuxeo.ecm.shell.commands.repository;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.nuxeo.ecm.shell.CommandDescriptor;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.header.CommandHeader;
import org.nuxeo.ecm.shell.header.GroovyHeaderExtractor;
import org.nuxeo.ecm.shell.header.HeaderExtractor;
import org.nuxeo.ecm.shell.header.PyHeaderExtractor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptingCommand extends AbstractCommand {

    protected static final ScriptEngineManager scriptMgr = new ScriptEngineManager();

    protected static final Map<String,HeaderExtractor> extractors= new HashMap<String, HeaderExtractor>();

    static {
        extractors.put("groovy", new GroovyHeaderExtractor());
        extractors.put("py", new PyHeaderExtractor());
    }

    protected final CommandDescriptor descriptor; // used to update descriptor when script is modified
    protected CompiledScript script;
    protected final File file;
    protected long lastModified = 0;
    protected CommandHeader header;

    public ScriptingCommand(CommandDescriptor descriptor, File file) {
        this.descriptor = descriptor;
        this.file = file;
        lastModified = file.lastModified();
        //getScript(); // force script compil
    }

    public CommandHeader getCommandHeader() throws Exception {
        getScript();
        return header;
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        getScript(); // force script compil if needed
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

    public static String getExtension(File file) {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p > -1) {
            return name.substring(p+1);
        }
        return "";
    }

    public CompiledScript getScript() throws ScriptException, IOException, ParseException {
        if (file.lastModified() > lastModified) {
            script = null;
        }
        if (script == null) {
            String ext = getExtension(file);
            ScriptEngine engine = scriptMgr.getEngineByExtension(ext);
            script = compileScript(engine, file);
            HeaderExtractor extractor = extractors.get(ext);
            if (extractor != null) {
                FileReader reader = new FileReader(file);
                try {
                    header = extractor.extractHeader(reader);
                } finally {
                  reader.close();
                }
            }
        }
        return script;
    }

}
