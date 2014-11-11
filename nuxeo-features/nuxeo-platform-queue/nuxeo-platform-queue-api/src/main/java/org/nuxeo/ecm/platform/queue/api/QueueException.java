/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     \Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>'
 */
package org.nuxeo.ecm.platform.queue.api;

/**
 * Generic queue exception.
 * 
 * @author \Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>'
 * 
 */
public class QueueException extends Exception {

    private static final long serialVersionUID = 1L;

    QueueContent content;

    public QueueException(String message, Throwable e) {
        super(message, e);
    }

    public QueueException(String message) {
        super(message);
    }

    public QueueException(String message, Throwable e, QueueContent content) {
        super("Queue exception on queue " + content.getDestination() + ":"
                + content.getName() + "\n" + message, e);
        this.content = content;
    }

    public QueueException(String message, QueueContent content) {
        super("Queue exception on queue " + content.getDestination() + ":"
                + content.getName() + "\n" + message);
        this.content = content;
    }

}
