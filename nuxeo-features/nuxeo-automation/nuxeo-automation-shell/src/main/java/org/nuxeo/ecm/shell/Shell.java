/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General public abstract License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General public abstract License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.nuxeo.ecm.shell.cmds.GlobalCommands;
import org.nuxeo.ecm.shell.cmds.Interactive;
import org.nuxeo.ecm.shell.fs.FileSystem;
import org.nuxeo.ecm.shell.impl.DefaultCompletorProvider;
import org.nuxeo.ecm.shell.impl.DefaultConsole;
import org.nuxeo.ecm.shell.impl.DefaultValueAdapter;
import org.nuxeo.ecm.shell.utils.StringUtils;

/**
 * 
 * parse args if no cmd attempt to read from stdin a list of cmds or from a
 * faile -f if cmd run it. A cmd line instance is parsing a single command.
 * parsed data is injected into the command and then the command is run. a cmd
 * type is providing the info on how a command is injected. top level params
 * are: -h help -u username -p password -f batch file - batch from stdin
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public abstract class Shell {

    /**
     * The shell instance
     */
    private static Shell shell;

    public static Shell get() {
        if (shell == null) {
            String caps = System.getProperty("shell.capabilities");
            if (caps == null) {
                caps = "automation";
            }
            shell = loadShell(caps);
        }
        return shell;
    }

    @SuppressWarnings("rawtypes")
    public static Shell loadShell(String capabilities) {
        if (capabilities == null) {
            throw new IllegalArgumentException("Capabilities are required");
        }
        String[] caps = StringUtils.split(capabilities, ',', true);
        ShellFactory factory = null;
        ServiceLoader<ShellFactory> loader = ServiceLoader.load(
                ShellFactory.class, Shell.class.getClassLoader());
        Iterator<ShellFactory> it = loader.iterator();
        while (it.hasNext()) {
            factory = it.next();
            if (factory.hasCapabilities(caps)) {
                return factory.getShell();
            }
        }
        throw new ShellException(
                "No shell found with the requested capabilities: "
                        + capabilities);
    }

    protected LinkedHashMap<String, String> mainArgs;

    protected CompositeCompletorProvider completorProvider;

    protected CompositeValueAdapter adapter;

    protected ShellConsole console;

    protected Map<String, CommandRegistry> cmds;

    protected CommandRegistry activeRegistry;

    protected Map<String, Object> ctx;

    protected Map<Class<?>, Object> ctxObjects;

    protected Shell() {
        if (shell != null) {
            throw new ShellException("Shell already loaded");
        }
        shell = this;

        activeRegistry = GlobalCommands.INSTANCE;
        cmds = new HashMap<String, CommandRegistry>();
        ctx = new HashMap<String, Object>();
        ctxObjects = new HashMap<Class<?>, Object>();
        ctxObjects.put(Shell.class, this);
        adapter = new CompositeValueAdapter();
        console = createConsole();
        completorProvider = new CompositeCompletorProvider();

        addCompletorProvider(new DefaultCompletorProvider());
        addValueAdapter(new DefaultValueAdapter());
        addRegistry(GlobalCommands.INSTANCE);
    }

    public LinkedHashMap<String, String> getMainArguments() {
        return mainArgs;
    }

    public void main(String[] args) throws Exception {
        mainArgs = collectArgs(args);
        String path = mainArgs.get("-f");
        if (path != null) {
            FileInputStream in = new FileInputStream(new File(path));
            List<String> lines = null;
            try {
                lines = FileSystem.readAndMergeLines(in);
            } finally {
                in.close();
            }
            runBatch(lines);
        } else if (mainArgs.get("-e") != null) {
            String[] cmds = StringUtils.split(mainArgs.get("-e"), ';', true);
            runBatch(Arrays.asList(cmds));
        } else if (mainArgs.get("-") != null) { // run batch from stdin
            List<String> lines = FileSystem.readAndMergeLines(System.in);
            runBatch(lines);
        } else {
            hello();
            run(Interactive.class.getAnnotation(Command.class).name());
        }
    }

    public LinkedHashMap<String, String> collectArgs(String[] args) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        if (args == null || args.length == 0) {
            return map;
        }
        String key = null;
        int k = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (key != null) {
                    map.put(key, "true");
                }
                key = args[i];
            } else if (key != null) {
                map.put(key, args[i]);
                key = null;
            } else {
                map.put("#" + (++k), args[i]);
                key = null;
            }
        }
        if (key != null) {
            map.put(key, "true");
        }
        return map;
    }

    public String[] parse(String cmdline) {
        return parse(cmdline.trim().toCharArray());
    }

    public String[] parse(char[] cbuf) {
        ArrayList<String> result = new ArrayList<String>();
        StringBuilder buf = new StringBuilder();
        boolean esc = false;
        char quote = 0;
        for (int i = 0; i < cbuf.length; i++) {
            char c = cbuf[i];
            if (esc) {
                esc = false;
                buf.append(c);
                continue;
            }
            switch (c) {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                if (quote != 0) {
                    buf.append(c);
                } else if (buf.length() > 0) {
                    result.add(buf.toString());
                    buf = new StringBuilder();
                }
                break;
            case '"':
                if (quote == '"') {
                    quote = 0;
                    result.add(buf.toString());
                    buf = new StringBuilder();
                } else if (buf.length() > 0) {
                    buf.append(c);
                } else {
                    quote = c;
                }
                break;
            case '\'':
                if (quote == '\'') {
                    quote = 0;
                    result.add(buf.toString());
                    buf = new StringBuilder();
                } else if (buf.length() > 0) {
                    buf.append(c);
                } else {
                    quote = c;
                }
                break;
            case '\\':
                esc = true;
                break;
            default:
                buf.append(c);
                break;
            }
        }
        if (buf.length() > 0) {
            result.add(buf.toString());
        }
        return result.toArray(new String[result.size()]);
    }

    protected ShellConsole createConsole() {
        return new DefaultConsole();
    }

    public void addValueAdapter(ValueAdapter adapter) {
        this.adapter.addAdapter(adapter);
    }

    public void addCompletorProvider(CompletorProvider provider) {
        this.completorProvider.addProvider(provider);
    }

    @SuppressWarnings("unchecked")
    public <T> T getContextObject(Class<T> type) {
        return (T) ctxObjects.get(type);
    }

    public <T> void putContextObject(Class<T> type, T instance) {
        ctxObjects.put(type, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeContextObject(Class<T> type) {
        return (T) ctxObjects.remove(type);
    }

    public CompletorProvider getCompletorProvider() {
        return completorProvider;
    }

    public void addRegistry(CommandRegistry reg) {
        cmds.put(reg.getName(), reg);
    }

    public CommandRegistry removeRegistry(String key) {
        return cmds.remove(key);
    }

    public CommandRegistry getRegistry(String name) {
        return cmds.get(name);
    }

    public CommandRegistry[] getRegistries() {
        return cmds.values().toArray(new CommandRegistry[cmds.size()]);
    }

    public String[] getRegistryNames() {
        CommandRegistry[] regs = getRegistries();
        String[] result = new String[regs.length];
        for (int i = 0; i < regs.length; i++) {
            result[i] = regs[i].getName();
        }
        return result;
    }

    public CommandRegistry getActiveRegistry() {
        return activeRegistry;
    }

    /**
     * Mark an already registered command registry as the active one.
     * 
     * @param name
     * @return
     */
    public CommandRegistry setActiveRegistry(String name) {
        CommandRegistry old = activeRegistry;
        activeRegistry = getRegistry(name);
        if (activeRegistry == null) {
            activeRegistry = old;
            getConsole().println("No such namespace: " + name);
            return null;
        }
        return old;
    }

    public ShellConsole getConsole() {
        return console;
    }

    public void setConsole(ShellConsole console) {
        this.console = console;
    }

    public ValueAdapter getValueAdapter() {
        return adapter;
    }

    public Object getProperty(String key) {
        return ctx.get(key);
    }

    public Object getProperty(String key, Object defaultValue) {
        Object v = ctx.get(key);
        return v == null ? defaultValue : v;
    }

    public void setProperty(String key, Object value) {
        ctx.put(key, value);
    }

    public void runBatch(List<String> lines) throws ShellException {
        for (String line : lines) {
            run(parse(line));
        }
    }

    public void run(String cmdline) throws ShellException {
        run(parse(cmdline));
    }

    public void run(String... line) throws ShellException {
        Runnable cmd = newCommand(line);
        if (cmd != null) {
            run(cmd);
        }
    }

    public void run(Runnable cmd) throws ShellException {
        cmd.run();
    }

    public Runnable newCommand(String cmdline) throws ShellException {
        return newCommand(parse(cmdline));
    }

    public Runnable newCommand(String... line) throws ShellException {
        if (line.length == 0) {
            return null;
        }
        CommandType type = activeRegistry.getCommandType(line[0]);
        if (type == null) {
            throw new ShellException("Unknown command: " + line[0]);
        }
        return type.newInstance(this, line);
    }

    public void hello() throws IOException {
        InputStream in = Shell.class.getClassLoader().getResourceAsStream(
                "META-INF/hello.txt");
        if (in == null) {
            System.out.println("Welcome to " + getClass().getSimpleName() + "!");
            System.out.println("Type \"help\" for more information.");
        } else {
            try {
                String content = FileSystem.readContent(in);
                System.out.println(content);
            } finally {
                in.close();
            }
        }
    }

    public void bye() {
        console.println("Bye.");
    }

}
