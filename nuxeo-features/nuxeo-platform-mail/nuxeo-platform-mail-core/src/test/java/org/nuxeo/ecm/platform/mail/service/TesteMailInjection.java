/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 *
 * $Id:
 */

package org.nuxeo.ecm.platform.mail.service;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.action.Visitor;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Laurent Doguin
 *
 */
public class TesteMailInjection extends
        SQLRepositoryTestCase {

    protected MailService mailService;

    protected String incomingDocumentType;

    protected DocumentModel mailFolder1;

    protected DocumentModel mailFolder2;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.mail");
        deployBundle("org.nuxeo.ecm.platform.mail.types");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        mailService = Framework.getService(MailService.class);
        assertNotNull(mailService);
        openSession();
        createMailFolders();
    }

    public void testMailUnicityCheck() throws Exception {
        assertNotNull(session.getDocument(new PathRef("/mailFolder1")));
        assertNotNull(session.getDocument(new PathRef("/mailFolder2")));
        injectEmail("data/test_mail.eml", mailFolder1.getPathAsString());
        DocumentModelList children = session.getChildren(mailFolder1.getRef());
        assertNotNull(children);
        assertTrue(!children.isEmpty());
        assertEquals(1, children.size());
        injectEmail("data/test_mail2.eml", mailFolder1.getPathAsString());
        children = session.getChildren(mailFolder1.getRef());
        assertEquals(2, children.size());
        injectEmail("data/test_mail.eml", mailFolder1.getPathAsString());
        children = session.getChildren(mailFolder1.getRef());
        // size won't change because same mail has been injected
        assertEquals(2, children.size());
        // inject previously injected mail in another mail folder
        injectEmail("data/test_mail.eml", mailFolder2.getPathAsString());
        injectEmail("data/test_mail2.eml", mailFolder2.getPathAsString());
        children = session.getChildren(mailFolder2.getRef());
        assertNotNull(children);
        assertTrue(!children.isEmpty());
        assertEquals(2, children.size());
    }

    private void injectEmail(String filePath, String parentPath) throws Exception {
        MessageActionPipe pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        Visitor visitor = new Visitor(pipe);
        ExecutionContext initialExecutionContext = new ExecutionContext();
        assertNotNull(session.getSessionId());
        initialExecutionContext.put(
                MailCoreConstants.CORE_SESSION_ID_KEY,
                session.getSessionId());
        initialExecutionContext.put(
                MailCoreConstants.MIMETYPE_SERVICE_KEY,
                Framework.getLocalService(MimetypeRegistry.class));
        initialExecutionContext.put(PARENT_PATH_KEY,
                parentPath);

        Message[] messages = new Message[] { getSampleMessage(filePath) };

        visitor.visit(messages, initialExecutionContext);
    }

    private Message getSampleMessage(String filePath) throws Exception {
        InputStream stream = new FileInputStream(getTestMailSource(filePath));
        MimeMessage msg = new MimeMessage((Session) null, stream);
        return msg;
    }

    private String getTestMailSource(String filePath) {
        return FileUtils.getResourcePathFromContext(filePath);
    }

    private void createMailFolders() throws ClientException {
        mailFolder1 = session.createDocumentModel("/", "mailFolder1", MailCoreConstants.MAIL_FOLDER_TYPE);
        session.createDocument(mailFolder1);
        session.saveDocument(mailFolder1);
        mailFolder2 = session.createDocumentModel("/", "mailFolder2", MailCoreConstants.MAIL_FOLDER_TYPE);
        session.createDocument(mailFolder2);
        session.saveDocument(mailFolder2);
        session.save();
    }

}
