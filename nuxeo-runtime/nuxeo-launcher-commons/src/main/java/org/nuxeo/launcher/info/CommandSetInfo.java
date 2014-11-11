/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mguillaume
 */

package org.nuxeo.launcher.info;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "commands")
/**
 * @since 5.6
 */
public class CommandSetInfo {

    public CommandSetInfo() {
    }

    @XmlElement(name = "command")
    public List<CommandInfo> commands = new ArrayList<>();

    /**
     * @param cmdType Command type. See constants in {@link CommandInfo}
     * @return new {@link CommandInfo} added to commands
     */
    public CommandInfo newCommandInfo(String cmdType) {
        CommandInfo cmdInfo = new CommandInfo(cmdType);
        commands.add(cmdInfo);
        return cmdInfo;
    }

    /**
     * Log commands in error
     *
     * @since 5.7
     */
    public void log() {
        log(false);
    }

    /**
     * Log full content of the command set (parse commands and their content)
     *
     * @since 5.7
     */
    public void log(boolean debug) {
        for (CommandInfo commandInfo : commands) {
            commandInfo.log(debug);
        }
    }
}
