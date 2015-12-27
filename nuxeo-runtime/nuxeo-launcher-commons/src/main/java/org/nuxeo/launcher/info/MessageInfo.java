/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
