/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.CLASS)
public class CleanupLevelClassTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    // the order of execution of the test methods isn't guaranteed by JUnit and
    // changed between Java 6 and Java 7, so the test decides order on its own

    public static int phase;

    @BeforeClass
    public static void beforeClass() {
        phase = 0;
    }

    @Test
    public void testA() throws Exception {
        runTest();
    }

    @Test
    public void testB() throws Exception {
        runTest();
    }

    public void runTest() throws Exception {
        switch (++phase) {
        case 1:
            firstTestToCreateADoc();
            break;
        case 2:
            docStillExists();
            break;
        }
    }

    public void firstTestToCreateADoc() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "default-domain", "Domain");
        doc.setProperty("dublincore", "title", "Domain");
        doc = session.createDocument(doc);
        session.save();
        assertTrue(session.exists(new PathRef("/default-domain")));

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();

        assertTrue(session.exists(new PathRef("/default-domain")));
    }

    public void docStillExists() throws Exception {
        assertTrue(session.exists(new PathRef("/default-domain")));
    }

}
