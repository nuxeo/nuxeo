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

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "command")
/**
 * @since 5.6
 */
public class CommandInfo {

    public static final String CMD_UNKNOWN = "unknown";

    public static final String CMD_LIST = "list";

    public static final String CMD_ADD = "add";

    public static final String CMD_INSTALL = "install";

    public static final String CMD_UNINSTALL = "uninstall";

    public static final String CMD_REMOVE = "remove";

    public static final String CMD_RESET = "reset";

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
    public Integer exitCode;

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

}
