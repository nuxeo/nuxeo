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
 *     mguillaume
 */

package org.nuxeo.launcher.info;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
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
    public List<MessageInfo> messages = new ArrayList<>();

    @XmlElementWrapper(name = "packages")
    @XmlElement(name = "package")
    public List<PackageInfo> packages = new ArrayList<>();

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
            sb.append("* Pending action: ").append(name);
        } else {
            sb.append("* ").append(name);
        }
        if (id != null) {
            sb.append(" [").append(id).append("]");
        }
        if (param != null) {
            sb.append(" (").append(param).append(")");
        }
        for (PackageInfo packageInfo : packages) {
            String[] ex = new String[] { "description" };
            String info = new ReflectionToStringBuilder(packageInfo, SHORT_PREFIX_STYLE).setExcludeFieldNames(ex)
                                                                                        .toString();
            sb.append("\n\t")
              .append(info);
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
