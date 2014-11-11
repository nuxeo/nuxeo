/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.test;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Constants;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

/**
 * PostCommitEventListenerTest test ScriptingPostCommitEventListener
 *
 * @author <a href="mailto:jt@nuxeo.com">Julien THIMONIER</a>
 */
public class PostCommitEventListenerTest extends SQLRepositoryTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        openSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    /**
     * The script listener will update this counter
     */
    public static int SCRIPT_CNT = 0;

    @Test
    public void testScripts() throws Exception {
        deployContrib(Constants.CORE_TEST_TESTS_BUNDLE,
                "test-PostCommitListeners.xml");
        assertEquals(0, SCRIPT_CNT);

        EventContextImpl customContext = new EventContextImpl(null, null);
        customContext.setProperty("cle", "valeur");
        customContext.setProperty("cle2", "valeur2");

        EventService service = Framework.getService(EventService.class);
        service.fireEvent("test", customContext);
        assertEquals(0, SCRIPT_CNT);

        service.fireEvent("test1", customContext);
        assertEquals(0, SCRIPT_CNT);

        // this one is filtered out
        service.fireEvent("some-event", customContext);
        assertEquals(0, SCRIPT_CNT);

        session.save();
        waitForAsyncCompletion();
        assertEquals(2, SCRIPT_CNT);
    }

    @Test
    public void testShallowFiltering() throws Exception {
        deployContrib(Constants.CORE_TEST_TESTS_BUNDLE,
                "test-ShallowFilteringPostCommitListeners.xml");
        DocumentModel doc = session.createDocumentModel("/", "empty", "Document");
        doc = session.createDocument(doc);
        ShallowFilterPostCommitEventListener.handledCount = 0;
        session.save();
        waitForAsyncCompletion();
        assertEquals(1, ShallowFilterPostCommitEventListener.handledCount);
    }

}
