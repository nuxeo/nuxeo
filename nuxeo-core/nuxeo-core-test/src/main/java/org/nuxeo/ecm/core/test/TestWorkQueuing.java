/*******************************************************************************
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
