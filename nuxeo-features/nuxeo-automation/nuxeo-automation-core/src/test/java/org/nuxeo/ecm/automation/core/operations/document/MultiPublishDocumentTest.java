/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class MultiPublishDocumentTest {

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    protected DocumentModel folder;

    protected DocumentModel section;

    protected DocumentModel section2;

    protected DocumentModel fileToPublish;

    @Before
    public void initRepo() throws Exception {

        folder = session.createDocumentModel("/", "Folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);
        session.save();

        section = session.createDocumentModel("/", "Section", "Section");
        section.setPropertyValue("dc:title", "Section");
        section = session.createDocument(section);
        session.save();

        section2 = session.createDocumentModel("/", "Section2", "Section");
        section2.setPropertyValue("dc:title", "Section");
        section2 = session.createDocument(section);
        session.save();

        fileToPublish = session.createDocumentModel("/Folder", "FileToPublish", "File");
        fileToPublish.setPropertyValue("dc:title", "FileToPublish");
        fileToPublish = session.createDocument(fileToPublish);
        session.save();

    }

    @Test
    public void testMultiPublishDocumentWithStringTarget() throws Exception {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(fileToPublish);
        OperationChain chain = new OperationChain("multiPublishDocumentWithStringInput");
        chain.add(FetchContextDocument.ID);
        chain.add(MultiPublishDocument.ID).set("target", section.getId() + "," + section2.getId());
        service.run(ctx, chain);

        assertEquals(1, session.getChildren(section.getRef()).size());
        assertEquals(1, session.getChildren(section2.getRef()).size());
    }

    @Test
    public void testMultiPublishDocumentWithArrayTarget() throws Exception {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(fileToPublish);
        OperationChain chain = new OperationChain("multiPublishDocumentWithArrayInput");
        chain.add(FetchContextDocument.ID);
        chain.add(MultiPublishDocument.ID).set("target",
                new ArrayList<>(Arrays.asList(section.getId(), section2.getId())));
        service.run(ctx, chain);

        assertEquals(1, session.getChildren(section.getRef()).size());
        assertEquals(1, session.getChildren(section2.getRef()).size());
    }
}
