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

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "message")
public class MessageInfo {

    static final Log log = LogFactory.getLog(MessageInfo.class);

    public MessageInfo() {
    }

    /**
     * @since 5.7
     */
    public MessageInfo(int level, String message) {
        this.level = level;
        this.message = message;
    }

    /**
     * @see org.apache.commons.logging.impl.SimpleLog levels
     */
    @XmlAttribute()
    public int level;

    @XmlAttribute()
    public Date time = new Date();

    @XmlAttribute()
    public String message;

    /**
     * Log content of the message info
     *
     * @since 5.7
     */
    public void log() {
        String msg = "\t" + message;
        switch (level) {
        case SimpleLog.LOG_LEVEL_TRACE:
            log.trace(msg);
            break;
        case SimpleLog.LOG_LEVEL_DEBUG:
            log.debug(msg);
            break;
        case SimpleLog.LOG_LEVEL_INFO:
            log.info(msg);
            break;
        case SimpleLog.LOG_LEVEL_WARN:
            log.warn(msg);
            break;
        case SimpleLog.LOG_LEVEL_ERROR:
            log.error(msg);
            break;
        case SimpleLog.LOG_LEVEL_FATAL:
            log.fatal(msg);
            break;
        default:
        }
    }

}
