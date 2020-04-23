/*
 * (C) Copyright 2014-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.core.event.test;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TransitionTest {

    @Inject
    protected CoreSession session;

    @Test
    public void itCanAddACommentWhenFollowingTransition() {
        DocumentModel doc = session.createDocumentModel("/", "myDoc", "File");
        doc = session.createDocument(doc);

        doc.putContextData("comment", "a comment");

        try (CapturingEventListener listener = new CapturingEventListener(LifeCycleConstants.TRANSITION_EVENT)) {
            session.followTransition(doc, "approve");
            assertEquals("a comment", getLastComment(listener));
        }
    }

    @Test
    public void itCanModifyACommentWhenModifyingADocument() {
        try (CapturingEventListener listener = new CapturingEventListener(LifeCycleConstants.TRANSITION_EVENT)) {
            DocumentModel doc = session.createDocumentModel("/", "myDoc", "File");
            doc = session.createDocument(doc);

            doc.setPropertyValue("dc:title", "title");
            doc.putContextData("comment", "a comment");
            doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);

            session.followTransition(doc, "approve");
            assertEquals("a comment", getLastComment(listener));

            doc = session.saveDocument(doc);

            doc.setPropertyValue("dc:title", "new title");
            doc.putContextData("comment", "b comment");

            session.saveDocument(doc);

            assertEquals("b comment", getLastComment(listener));
        }
    }

    private String getLastComment(CapturingEventListener listener) {
        return listener.getLastCapturedEvent(LifeCycleConstants.TRANSITION_EVENT)
                       .map(event -> (String) event.getContext().getProperty("comment"))
                       .orElseThrow(() -> new AssertionError("Unable to find last comment"));
    }

}
