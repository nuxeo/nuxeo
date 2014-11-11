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

import java.net.URI;


/**
 * Generic queue error.
 *
 * @author Stephane Lacoin (aka matic)
 *
 */
public class QueueError extends Error {

    private static final long serialVersionUID = 1L;

    public final URI name;

    public QueueError(String message, Throwable e) {
        super(message, e);
        name = null;
    }

    public QueueError(String message) {
        super(message);
        name = null;
    }

    public QueueError(String message, URI name) {
        super("Queue error on  " +  name + "\n" + message);
        this.name = name;
    }
    public QueueError(String message, Throwable e, URI name) {
        super("Queue error on  " +  name + "\n" + message, e);
        this.name = name;
    }

    public QueueError(String message, Throwable e, QueueInfo<?> info) {
        super("Queue error on queue " + info+ "\n" + message, e);
        this.name = info.getName();
    }

    public QueueError(String message, QueueInfo<?> info) {
        super("Queue error on queue " + info + "\n" + message);
        this.name = info.getName();
    }

}
