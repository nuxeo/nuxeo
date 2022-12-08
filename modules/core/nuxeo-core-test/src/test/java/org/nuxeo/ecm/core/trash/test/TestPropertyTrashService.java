/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.trash.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.trash.TrashService.ABOUT_TO_TRASH;
import static org.nuxeo.ecm.core.api.trash.TrashService.ABOUT_TO_UNTRASH;

import org.junit.Test;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 10.1
 */
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-property-override.xml")
public class TestPropertyTrashService extends AbstractTestTrashService {

    @Test
    public void testAboutToTrashEvent() {
        var doc = session.createDocumentModel("/", "test", "File");
        try (CapturingEventListener listener = new CapturingEventListener(ABOUT_TO_TRASH, ABOUT_TO_UNTRASH)) {
            doc = session.createDocument(doc);
            trashService.trashDocument(doc);

            assertEquals(1, listener.getCapturedEvents().size());
            assertTrue(listener.getCapturedEvents().get(0).isInline());
            assertEquals(ABOUT_TO_TRASH, listener.getCapturedEvents().get(0).getName());

            trashService.untrashDocument(doc);

            assertEquals(2, listener.getCapturedEvents().size());
            assertTrue(listener.getCapturedEvents().get(1).isInline());
            assertEquals(ABOUT_TO_UNTRASH, listener.getCapturedEvents().get(1).getName());
        }
    }

}
