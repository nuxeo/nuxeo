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
 *     bstefanescu, jcarsique
 *
 * $Id$
 */

package org.nuxeo.ecm.shell;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * A command descriptor. This describes command arguments and help.
 * <p>
 * A command descriptor may be lazy this means when you need to be sure command definition is loaded
 * we must call {@link #load()} before accessing command definition.
 * <p>
 * When instantiating a command using {@link #newInstance()} the definition
 * will be automatically loaded if needed.
 * <p>
 * Laziness avoids compiling script commands at startup.
 *
 * TODO: support for "bundle:" scripts
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("command")
public class DefaultCommandDescriptor implements CommandDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<?> klass;

    protected String[] aliases;

    @XNode("description")
    protected String description = "N/A";

    @XNode("help")
    protected String help = "N/A";

    @XNodeList(value="options/option", type=CommandOption[].class,
            componentType=CommandOption.class)
    public CommandOption[] options;

    @XNodeList(value="params/param", type=CommandParameter[].class,
            componentType=CommandParameter.class)
    public CommandParameter[] params;


    public DefaultCommandDescriptor() {
        // TODO Auto-generated constructor stub
    }

    public DefaultCommandDescriptor(String name, Class<?> klass) {
        this.name = name;
        this.klass = klass;
    }

    public boolean isDynamicScript() {
        return false;
    }

    public boolean hasOptions() {
        return options != null && options.length > 0;
    }

    public boolean hasArguments() {
        return params != null && params.length > 0;
    }

    @Override
    public String toString() {
        return name + " [" + klass + ']';
    }

    /**
     * Set alternate names.
     */
    @XNode("@alias")
    void setAlias(String alias) {
        aliases = StringUtils.split(alias, ',', true);
    }

    public String[] getAliases() {
        return aliases;
    }

    public String getDescription() {
        return description;
    }

    public String getHelp() {
        return help;
    }

    public String getName() {
        return name;
    }

    public CommandOption[] getOptions() {
        return options;
    }

    public CommandParameter[] getArguments() {
        return params;
    }

    public void setParams(CommandParameter[] params) {
        this.params = params;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public void setOptions(CommandOption[] options) {
        this.options = options;
    }

    public int compareTo(CommandDescriptor o) {
        return name.compareTo(o.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof DefaultCommandDescriptor)) {
            return false;
        }

        DefaultCommandDescriptor that = (DefaultCommandDescriptor) o;
        return that.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public Command newInstance() throws Exception {
        if (klass != null) {
            return (Command)klass.newInstance();
        } else {
            throw new IllegalStateException("Command implementation not defined : "+name);
        }
    }

}
