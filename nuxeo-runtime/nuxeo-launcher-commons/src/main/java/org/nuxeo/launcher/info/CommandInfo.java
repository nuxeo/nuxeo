/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "command")
/**
 * @since 5.6
 */
public class CommandInfo {

    static final Log log = LogFactory.getLog(CommandInfo.class);

    public static final String CMD_UNKNOWN = "unknown";

    public static final String CMD_LIST = "list";

    public static final String CMD_ADD = "add";

    public static final String CMD_INSTALL = "install";

    public static final String CMD_UNINSTALL = "uninstall";

    public static final String CMD_REMOVE = "remove";

    public static final String CMD_RESET = "reset";

    /**
     * @since 5.7
     */
    public static final String CMD_DOWNLOAD = "download";

    public static final String CMD_INIT = "init";

    /**
     * @since 5.7
     */
    public static final String CMD_SHOW = "show";

    public CommandInfo() {
    }

    public CommandInfo(String cmdType) {
        name = cmdType;
    }

    @XmlAttribute()
    public String name;

    @XmlAttribute()
    public String param;

    @XmlAttribute()
    public Integer exitCode = 0;

    @XmlAttribute()
    public String id;

    @XmlAttribute()
    public boolean pending = false;

    @XmlElementWrapper(name = "messages")
    @XmlElement(name = "message")
    public List<MessageInfo> messages = new ArrayList<MessageInfo>();

    @XmlElementWrapper(name = "packages")
    @XmlElement(name = "package")
    public List<PackageInfo> packages = new ArrayList<PackageInfo>();

    /**
     * @return new {@link MessageInfo} added to messages
     * @since 5.7
     */
    public MessageInfo newMessage() {
        MessageInfo messageInfo = new MessageInfo();
        messages.add(messageInfo);
        return messageInfo;
    }

    /**
     * @return new {@link MessageInfo} added to messages
     * @since 5.7
     */
    public MessageInfo newMessage(int level, String message) {
        MessageInfo messageInfo = new MessageInfo(level, message);
        messages.add(messageInfo);
        return messageInfo;
    }

    /**
     * @return new {@link MessageInfo} added to messages
     * @since 5.7
     */
    public MessageInfo newMessage(Exception e) {
        return newMessage(SimpleLog.LOG_LEVEL_ERROR, e);
    }

    /**
     * @return new {@link MessageInfo} added to messages
     * @since 5.7
     */
    public MessageInfo newMessage(int level, Exception e) {
        log.debug(e, e);
        return newMessage(level, e.getMessage());
    }

    /**
     * Log content of the command info
     *
     * @since 5.7
     */
    public void log(boolean debug) {
        StringBuilder sb = new StringBuilder();
        if (pending) {
            sb.append("* Pending action: " + name);
        } else {
            sb.append("* " + name);
        }
        if (id != null) {
            sb.append(" [" + id + "]");
        }
        if (param != null) {
            sb.append(" (" + param + ")");
        }
        for (PackageInfo packageInfo : packages) {
            sb.append("\n\t"
                    + new ReflectionToStringBuilder(packageInfo,
                            ToStringStyle.SHORT_PREFIX_STYLE).setExcludeFieldNames(
                            new String[] { "description" }).toString());
        }
        if (exitCode != 0 || debug) {
            if (exitCode != 0) {
                log.error(sb.toString());
            } else {
                log.info(sb.toString());
            }
            for (MessageInfo messageInfo : messages) {
                messageInfo.log();
            }
        }
    }

}
