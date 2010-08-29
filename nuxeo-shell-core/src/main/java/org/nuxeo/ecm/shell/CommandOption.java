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

package org.nuxeo.ecm.shell;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("option")
public class CommandOption {

    @XNode("@name")
    String name;

    @XNode("@shortcut")
    String shortcut;

    @XNode("@var")
    boolean isVariable = false;

    @XNode("@required")
    boolean isRequired = false;

    @XNode("@value")
    String defaultValue;

    @XNode("@command")
    String command;

    // the type will be used to lookup for a value completor.
    // The file type will use a "file" for auto-completion while the "doc" type a document name
    @XNode("@type")
    String type;

    @XContent
    String help;

    public CommandOption() {
    }

    public CommandOption(String name) {
        this.name = name;
    }

    public CommandOption(String name, String shortcut, String defaultValue) {
        this.name = name;
        this.shortcut = shortcut;
        this.defaultValue = defaultValue;
    }


    public void setIsRequired(boolean value) {
        isRequired = value;
    }

    public void setIsVariable(boolean value) {
        isVariable = value;
    }

    public void setIsFlag(boolean value) {
        isVariable = !value;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public boolean isVariable() {
        return isVariable;
    }

    public boolean isFlag() {
        return !isVariable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
