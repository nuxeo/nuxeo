/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Thierry Delprat</a>
 *
 * $Id:
 */

package org.nuxeo.ecm.platform.mail.service.automation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.action.Visitor;
import org.nuxeo.ecm.platform.mail.service.MailService;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author tiry
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.mail")
@Deploy("org.nuxeo.ecm.platform.mail.types")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.core.convert.api")
@Deploy("org.nuxeo.ecm.core.convert.plugins")
@Deploy("org.nuxeo.ecm.platform.mail.test:OSGI-INF/nxmail-automation-contrib.xml")
public class TestMailInjection {

    @Inject
    protected MailService mailService;

    @Inject
    protected CoreSession session;

    protected DocumentModel mailFolder1;

    @Before
    public void setUp() {
        createMailFolders();
    }

    @Test
    public void testMailImport() throws Exception {

        assertNotNull(session.getDocument(new PathRef("/mailFolder1")));
        injectEmail("data/test_mail.eml", mailFolder1.getPathAsString());
        DocumentModelList children = session.getChildren(mailFolder1.getRef());
        assertNotNull(children);
        assertTrue(!children.isEmpty());
        assertEquals(1, children.size());

        injectEmail("data/test_mail2.eml", mailFolder1.getPathAsString());
        children = session.getChildren(mailFolder1.getRef());
        assertEquals(2, children.size());

        injectEmail("data/test_mail_attachment.eml", mailFolder1.getPathAsString());
        children = session.getChildren(mailFolder1.getRef());
        assertEquals(3, children.size());

        DocumentModel mailWithAttachment = session.query(
                "select * from Document where dc:title = \"MailWithAttachment\"").get(0);
        BlobHolder bh = mailWithAttachment.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        List<Blob> blobs = bh.getBlobs();
        assertNotNull(blobs);
        assertEquals(4, blobs.size());
    }

    private void injectEmail(String filePath, String parentPath) throws Exception {
        MessageActionPipe pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        Visitor visitor = new Visitor(pipe);
        ExecutionContext initialExecutionContext = new ExecutionContext();
        assertNotNull(session.getSessionId());
        // 5.8 setup
        initialExecutionContext.put(MailCoreConstants.CORE_SESSION_KEY, session);
        initialExecutionContext.put(MailCoreConstants.MIMETYPE_SERVICE_KEY,
                Framework.getService(MimetypeRegistry.class));
        initialExecutionContext.put(PARENT_PATH_KEY, parentPath);

        Message[] messages = { getSampleMessage(filePath) };

        visitor.visit(messages, initialExecutionContext);
    }

    private Message getSampleMessage(String filePath) throws Exception {
        InputStream stream = new FileInputStream(getTestMailSource(filePath));
        return new MimeMessage(null, stream);
    }

    private String getTestMailSource(String filePath) {
        return FileUtils.getResourcePathFromContext(filePath);
    }

    private void createMailFolders() {
        mailFolder1 = session.createDocumentModel("/", "mailFolder1", MailCoreConstants.MAIL_FOLDER_TYPE);
        session.createDocument(mailFolder1);
        session.saveDocument(mailFolder1);
        session.save();
    }

}
