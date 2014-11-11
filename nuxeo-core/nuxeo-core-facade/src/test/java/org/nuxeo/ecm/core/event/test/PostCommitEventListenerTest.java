/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.net.URL;

import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * PostCommitEventListenerTest test ScriptingPostCommitEventListener
 *
 * @author <a href="mailto:jt@nuxeo.com">Julien THIMONIER</a>
 */
public class PostCommitEventListenerTest extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        openRepository();
    }

    /**
     * The script listener will update this counter
     */
    public static int SCRIPT_CNT = 0;

    public void testScripts() throws Exception {
        URL url = PostCommitEventListenerTest.class.getClassLoader().getResource(
                "test-PostCommitListeners.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        assertEquals(0, SCRIPT_CNT);
        EventContextImpl customContext = new EventContextImpl(null, null);
        customContext.setProperty("cle", "valeur");
        customContext.setProperty("cle2", "valeur2");

        EventService service = Framework.getService(EventService.class);
        service.fireEvent("test", customContext);
        assertEquals(0, SCRIPT_CNT);
        service.fireEvent("test1", customContext);
        assertEquals(0, SCRIPT_CNT);

        // POST Event Listener does not filter events, but receive an all event
        // bundle with all
        service.fireEvent("some-event", customContext);
        assertEquals(0, SCRIPT_CNT);
        getCoreSession().save();
        waitForAsyncExec();
        assertTrue(3 <= SCRIPT_CNT);

    }

    protected void waitForAsyncExec() {

        EventServiceImpl evtService = (EventServiceImpl) Framework.getLocalService(EventService.class);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (evtService.getActiveAsyncTaskCount() > 0) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
