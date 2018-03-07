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

import org.junit.Test;
import org.nuxeo.ecm.core.trash.LifeCycleTrashService;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @deprecated since 10.1 along with {@link LifeCycleTrashService}.
 */
@Deprecated
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-lifecycle-override.xml")
public class TestLifeCycleTrashService extends AbstractTestTrashService {

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-checkin-dontkeep.xml")
    public void testTrashCheckedInDocumentDontKeepCheckedIn() {
        doTestTrashCheckedInDocument(false);
    }

}
