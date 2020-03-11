/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     mguillaume, jcarsique
 */

package org.nuxeo.launcher.info;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "commands")
/**
 * @since 5.6
 */
public class CommandSetInfo {

    static final Log log = LogFactory.getLog(CommandSetInfo.class);

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
        if (commands.isEmpty()) {
            return;
        }
        if (debug) {
            log.debug("\nCommands debug dump:");
        } else {
            log.error("\nFailed commands:");
        }
        for (CommandInfo commandInfo : commands) {
            commandInfo.log(debug);
        }
    }
}
