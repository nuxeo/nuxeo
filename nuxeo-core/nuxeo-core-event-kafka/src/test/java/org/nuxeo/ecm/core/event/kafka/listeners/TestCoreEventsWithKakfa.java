/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.kafka.listeners;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.kafka.DummyEventListener;
import org.nuxeo.ecm.core.event.kafka.test.CoreWithKafkaFeature;
import org.nuxeo.ecm.core.event.pipe.dispatch.EventBundleDispatcher;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features({CoreWithKafkaFeature.class })
@LocalDeploy({"org.nuxeo.ecm.core.event.kafka.test:test-async-core-listeners.xml"})
public class TestCoreEventsWithKakfa {

    @Inject
    CoreSession session;

    @Inject
    EventService eventService;

    @Test
    public void testCoreEventsMarshalling() throws Exception {

        EventBundleDispatcher dispatcher = ((EventServiceImpl) eventService).getEventBundleDispatcher();
        // check that kafka pipe is indeed deployed !
        Assert.assertNotNull("Kafka pipe not deployed", dispatcher);

        DummyEventListener.init();

        DocumentModel doc = session.createDocumentModel("/", "test", "File");
        doc.setPropertyValue("dc:title", "Test");
        doc = session.createDocument(doc);
        Thread.sleep(100L);

        doc.setPropertyValue("dc:title", "Test Updated");
        doc.putContextData("testing", "tested");
        doc = session.saveDocument(doc);
        doc.setPropertyValue("dc:title", "Test Updated Again");
        doc = session.saveDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();

        eventService.waitForAsyncCompletion();

        TransactionHelper.startTransaction();

        Assert.assertEquals(3, DummyEventListener.events.size());

        Assert.assertEquals("documentCreated", DummyEventListener.events.get(0).getName());
        Assert.assertEquals("documentModified", DummyEventListener.events.get(1).getName());
        Assert.assertEquals("documentModified", DummyEventListener.events.get(2).getName());

        Assert.assertTrue(DummyEventListener.events.get(0).getTime() < DummyEventListener.events.get(1).getTime());

        DocumentEventContext ctx = (DocumentEventContext) DummyEventListener.events.get(0).getContext();
        Assert.assertTrue(ctx.getCoreSession()!=null);
        Assert.assertTrue(ctx.getSourceDocument().getId().equals(doc.getId()));
        Assert.assertTrue(ctx.getPrincipal()!=null);

        // check that event context properties are correctly marshalled !
        ctx = (DocumentEventContext) DummyEventListener.events.get(1).getContext();
        Assert.assertEquals("tested", ctx.getProperties().get("testing"));

    }
}
