/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.shell;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.nuxeo.ecm.shell.impl.DefaultCommandType;

/**
 * A command registry associated to a given domain name.
 * 
 * Registries are named so each registry is using a different namespace.
 * 
 * For example you can have a "local" and a "remote" namespace using different
 * command registries. Commands in local namespace are working on the filesystem
 * while the one in remote namespace are working on a remote server.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class CommandRegistry {

    protected String name;

    protected CommandRegistry parent;

    protected Map<String, CommandType> cmds;

    public CommandRegistry(CommandRegistry parent, String name) {
        cmds = new HashMap<String, CommandType>();
        this.parent = parent;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public CommandRegistry getParent() {
        return parent;
    }

    public void addCommandType(CommandType type) {
        cmds.put(type.getName(), type);
        String[] aliases = type.getAliases();
        if (aliases != null && aliases.length > 0) {
            for (String alias : aliases) {
                cmds.put(alias, type);
            }
        }
    }

    public void addAnnotatedCommand(Class<? extends Runnable> clazz) {
        addCommandType(DefaultCommandType.fromAnnotatedClass(clazz));
    }

    public CommandType getCommandType(String name) {
        CommandType type = cmds.get(name);
        if (type == null && parent != null) {
            type = parent.getCommandType(name);
        }
        return type;
    }

    public Set<CommandType> getCommandTypeSet() {
        HashSet<CommandType> set = new HashSet<CommandType>();
        if (parent != null) {
            set.addAll(parent.getCommandTypeSet());
        }
        set.addAll(cmds.values());
        return set;
    }

    public CommandType[] getCommandTypes() {
        Set<CommandType> set = getCommandTypeSet();
        CommandType[] ar = set.toArray(new CommandType[set.size()]);
        Arrays.sort(ar, new Comparator<CommandType>() {
            public int compare(CommandType o1, CommandType o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return ar;
    }

    protected void collectCommandTypesByNamespace(
            Map<String, Set<CommandType>> map) {
        if (parent != null) {
            parent.collectCommandTypesByNamespace(map);
        }
        TreeSet<CommandType> set = new TreeSet<CommandType>();
        for (CommandType cmd : cmds.values()) {
            set.add(cmd);
        }
        map.put(getName(), set);
    }

    public Map<String, Set<CommandType>> getCommandTypesByNamespace() {
        LinkedHashMap<String, Set<CommandType>> map = new LinkedHashMap<String, Set<CommandType>>();
        collectCommandTypesByNamespace(map);
        return map;
    }

    public Set<String> getCommandNameSet() {
        HashSet<String> set = new HashSet<String>();
        if (parent != null) {
            set.addAll(parent.getCommandNameSet());
        }
        set.addAll(cmds.keySet());
        return set;
    }

    /**
     * Get sorted command names including aliases
     * 
     * @return
     */
    public String[] getCommandNames() {
        Set<String> set = getCommandNameSet();
        String[] ar = set.toArray(new String[set.size()]);
        Arrays.sort(ar);
        return ar;
    }

    public String getPrompt(Shell shell) {
        return "> ";
    }

}
