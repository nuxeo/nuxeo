/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Lock;

public class TestMarkLogicLockUpdateBuilder extends AbstractTest {

    private static MarkLogicLockUpdateBuilder UPDATE_BUILDER;

    @BeforeClass
    public static void beforeClass() {
        AbstractTest.beforeClass();
        UPDATE_BUILDER = new MarkLogicLockUpdateBuilder(CLIENT.newXMLDocumentManager()::newPatchBuilder);
    }

    @Test
    public void testSetLock() throws Exception {
        Lock lock = new Lock("Administrator", MarkLogicHelper.deserializeCalendar("1970-01-01T00:00:00.000"));
        String patch = UPDATE_BUILDER.set(lock).toString();
        assertXMLFileAgainstString("lock-update-builder/set-lock.xml", patch);
    }

    @Test
    public void testDeleteLock() throws Exception {
        String patch = UPDATE_BUILDER.delete().toString();
        assertXMLFileAgainstString("lock-update-builder/delete-lock.xml", patch);

    }

}
