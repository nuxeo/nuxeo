/*******************************************************************************
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 ******************************************************************************/
package org.nuxeo.ecm.core.test;

import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.work.MemoryBlockingQueue;
import org.nuxeo.ecm.core.work.MemoryWorkQueuing;
import org.nuxeo.ecm.core.work.WorkHolder;
import org.nuxeo.ecm.core.work.WorkManagerImpl;
import org.nuxeo.ecm.core.work.WorkQueueDescriptorRegistry;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;

public class TestWorkQueuing extends MemoryWorkQueuing {

    public TestWorkQueuing(WorkManagerImpl mgr, WorkQueueDescriptorRegistry workQueueDescriptors) {
        super(mgr, workQueueDescriptors);
    }

    @Override
    protected BlockingQueue<Runnable> newBlockingQueue(WorkQueueDescriptor workQueueDescriptor) {
        return new MemoryBlockingQueue(workQueueDescriptor.getCapacity()) {
            @Override
            public void putElement(Runnable r) throws InterruptedException {
                super.putElement(clone(r));
            }

            Runnable clone(Runnable r) {
                Work original = WorkHolder.getWork(r);
                try {
                    return new WorkHolder(SerializationUtils.clone(original));
                } catch (SerializationException cause) {
                    throw new NuxeoException("Cannot serialize work of type " + original.getClass().getName());
                }
            }
        };
    }
}
