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
 *     anguenot
 *
 * $Id: SimpleTimer.java 28498 2008-01-05 11:46:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple timer used to measure execution time within code.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class SimpleTimer {

    private static final Log log = LogFactory.getLog(SimpleTimer.class);

    private Long start;

    private Long stop;

    private Long mark;

    public void start() {
        if (start == null) {
            start = mark = System.currentTimeMillis();
            log.debug("Timer has been just initialized");
        } else {
            log.warn("Timer already initialized");
        }
    }

    public void stop() {
        if (stop == null) {
            stop = System.currentTimeMillis();
            log.debug("Timer has been just stoped");
        } else {
            log.warn("Timer already stoped");
        }
    }

    public Long getDuration() throws TimerException {
        if (start == null) {
            throw new TimerException("Timer's not started");
        }
        if (stop == null) {
            throw new TimerException("Timer's not stoped.");
        }
        return stop - start;
    }

    public long getSpent() throws TimerException {
        if (start == null) {
            throw new TimerException("Timer's not started");
        }
        long now = System.currentTimeMillis();
        return now -start;
    }

    public String getSpent(String comment) throws TimerException {
        return comment + " : " + Long.toString(getSpent()) + "ms";
    }

    public String mark(String comment) {
        if (start == null) {
            return "Cannot start partial duration since timer is not started";
        }
        long now = System.currentTimeMillis();
        String cc = comment + " : " + Long.toString((now - mark)) + "ms";
        mark = now;
        return cc;
    }

    @Override
    public String toString() {
        try {
            long duration = getDuration();
            return " Duration :" + Long.toString(duration) + "ms";
        } catch (TimerException te) {
            return "Cannot compute compute elpased time for this timer...";
        }
    }
}
