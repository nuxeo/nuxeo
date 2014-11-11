/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class CleanupLevelClassTest {

    @Inject
    CoreSession session;

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
        switch(++phase) {
        case 1:
            firstTestToCreateADoc();
            break;
        case 2:
            docStillExists();
            break;
        }
    }

    public void firstTestToCreateADoc() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "default-domain",
                "Domain");
        doc.setProperty("dublincore", "title", "Default domain");
        doc = session.createDocument(doc);
        session.save();
        assertTrue(session.exists(new PathRef("/default-domain")));
    }

    public void docStillExists() throws Exception {
        assertTrue(session.exists(new PathRef("/default-domain")));
    }

}
