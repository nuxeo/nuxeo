/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.core;

import java.lang.reflect.Constructor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.ecm.platform.queue.api.QueueProcessor;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
@XObject("queue")
public class QueueDescriptor {

    @XNode("@name")
    public final String name = null;

    @XNode("@type")
    public final Class<?> contentType = null;

    @XNode("@processor")
    public final Class<QueueProcessor<?>> processorClass = null;

    @XNode("@persister")
    public final Class<QueuePersister<?>> persisterClass = null;


    public QueueProcessor<?> newProcessor() throws Exception {
        return processorClass.newInstance();
    }

    public QueuePersister<?> newPersister() throws Exception {
        Constructor<QueuePersister<?>> c = persisterClass.getDeclaredConstructor(String.class, Class.class);
        return c.newInstance(name, contentType);
    }

}
