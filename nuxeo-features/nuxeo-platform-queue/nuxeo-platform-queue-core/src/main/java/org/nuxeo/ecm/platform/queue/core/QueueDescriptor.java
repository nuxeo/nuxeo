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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.queue.api.QueueExecutor;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;

/**
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
@XObject("queue")
public class QueueDescriptor {

    @XNode("@name")
    String name;

    @XNode("@executor")
    Class<?> executorClass;

    @XNode("@persister")
    Class<?> persisterClass;

    public String getName() {
        return name;
    }

    public QueueExecutor newExecutorInstance() throws Exception {
        return (QueueExecutor) executorClass.newInstance();
    }

    public QueuePersister newPersisterInstance() throws Exception {
        return (QueuePersister) persisterClass.newInstance();
    }

}
