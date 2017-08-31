/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.core.event.test;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.common.collect.Sets;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TransitionTest implements EventListener {

    @Inject
    EventService eventService;

    @Inject
    CoreSession session;

    private String lastComment;

    protected EventListenerDescriptor eventDesc;

    @Before
    public void doBefore() {
        lastComment = null;
        eventDesc = new EventListenerDescriptor() {
            @Override
            public Set<String> getEvents() {
                return Sets.newHashSet(LifeCycleConstants.TRANSITION_EVENT);
            }

            @Override
            public EventListener asEventListener() {
                return TransitionTest.this;
            }

            @Override
            public void initListener() {

            }
        };
        eventService.addEventListener(eventDesc);
    }

    @After
    public void doAfter() {
        eventService.removeEventListener(eventDesc);
    }

    @Test
    public void itCanAddACommentWhenFollowingTransition() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "myDoc", "File");
        doc = session.createDocument(doc);

        doc.putContextData("comment", "a comment");

        session.followTransition(doc, "approve");

        assertEquals("a comment", lastComment);
    }

    @Test
    public void itCanModifyACommentWhenModifyingADocument() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "myDoc", "File");
        doc = session.createDocument(doc);

        doc.setPropertyValue("dc:title", "title");
        doc.putContextData("comment", "a comment");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);

        session.followTransition(doc, "approve");
        assertEquals("a comment", lastComment);

        doc = session.saveDocument(doc);

        doc.setPropertyValue("dc:title", "new title");
        doc.putContextData("comment", "b comment");

        session.saveDocument(doc);

        assertEquals("b comment", lastComment);
    }

    @Override
    public void handleEvent(Event event) {
        if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName())) {
            lastComment = (String) event.getContext().getProperty("comment");
        }
    }

}
