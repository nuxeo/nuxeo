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
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandDescriptor;
import org.nuxeo.ecm.shell.CommandOption;
import org.nuxeo.ecm.shell.CommandParameter;
import org.nuxeo.ecm.shell.header.CommandArgument;
import org.nuxeo.ecm.shell.header.CommandHeader;
import org.nuxeo.ecm.shell.header.CommandPattern;
import org.nuxeo.ecm.shell.header.GroovyHeaderExtractor;
import org.nuxeo.ecm.shell.header.HeaderExtractor;
import org.nuxeo.ecm.shell.header.PyHeaderExtractor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptingCommandDescriptor implements CommandDescriptor {

    protected static final Map<String,HeaderExtractor> extractors = new HashMap<String, HeaderExtractor>();

    static {
        extractors.put("groovy", new GroovyHeaderExtractor());
        extractors.put("py", new PyHeaderExtractor());
        extractors.put("js", new GroovyHeaderExtractor());
    }

    protected CommandHeader header;

    protected final String name;
    protected final File file;
    protected long lastModified = 0;
    protected ScriptingCommand cmd;


    public ScriptingCommandDescriptor(File file) {
        this.file = file;
        name = FileUtils.getFileNameNoExt(file.getName());
    }

    public String[] getAliases() {
        return null; //TODO
    }


    public CommandOption[] getOptions() {
        load();
        if (header == null) {
            return null;
        }
        CommandOption[] opts = new CommandOption[header.pattern.options.size()];
        int i=0;
        for (org.nuxeo.ecm.shell.header.CommandOption o : header.pattern.options) {
            opts[i++] = toCommandOption(o);
        }
        return opts;
    }

    public CommandParameter[] getArguments() {
        load();
        if (header == null) {
            return null;
        }
        CommandParameter[] args = new CommandParameter[header.pattern.args.size()];
        int i=0;
        for (CommandArgument a : header.pattern.args) {
            args[i] = toCommandParam(a, i);
            i++;
        }
        return args;
    }

    protected CommandOption toCommandOption(org.nuxeo.ecm.shell.header.CommandOption o) {
        CommandOption opt = new CommandOption();
        opt.setName(o.names[0]);
        opt.setCommand(getName());
        opt.setDefaultValue(o.defaultValue);
        opt.setType(o.type);
        opt.setIsVariable(o.type != null);
        opt.setShortcut(o.names.length > 1 ? o.names[1] : null);
        opt.setIsRequired(o.isRequired);
        return opt;
    }

    protected CommandParameter toCommandParam(CommandArgument a, int index) {
        CommandParameter param = new CommandParameter();
        param.type = a.type;
        param.index = index;
        return param;
    }

    public String getDescription() {
        load();
        if (header == null) {
            return "N/A";
        }
        return header.description;
    }

    public String getHelp() {
        load();
        if (header == null) {
            return "N/A";
        }
        return header.help;
    }

    public String getName() {
        return name;
    }

    public boolean hasArguments() {
        CommandParameter[] args = getArguments();
        return args != null && args.length > 0;
    }

    public boolean hasOptions() {
        CommandOption[] opts = getOptions();
        return opts != null && opts.length > 0;
    }

    public boolean isDynamicScript() {
        return true;
    }

    public Command newInstance() throws Exception {
        return getScriptingCommand();
    }

    public int compareTo(CommandDescriptor o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        return header.toString();
    }

    public void load() {
        try {
            getScriptingCommand();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ScriptingCommand getScriptingCommand() throws Exception {
        if (file.lastModified() > lastModified) {
            String ext = FileUtils.getFileExtension(file.getName());
            if (ext == null) {
                throw new IllegalArgumentException(
                        "Script File must have a valid extension: " + file.getAbsolutePath());
            }
            cmd = new ScriptingCommand(file);
            HeaderExtractor extractor = extractors.get(ext);
            if (extractor != null) {
                FileReader reader = new FileReader(file);
                try {
                    header = extractor.extractHeader(reader);
                    if (header != null) {
                        if (header.description == null) {
                            header.description = "N/A";
                        }
                        if (header.help == null) {
                            header.help = "N/A";
                        }
                        if (header.pattern == null) {
                            header.pattern = new CommandPattern();
                            header.pattern.names = new String[] {getName()};
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
        return cmd;
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
