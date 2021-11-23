/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Vincent Dutat <vdutat@nuxeo.com>
 */
package org.nuxeo.ecm.platform.mail.adapter;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.mail.MailFeature;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2021.9
 */
@RunWith(FeaturesRunner.class)
@Features(MailFeature.class)
public class TestMailMessageBlobHolder {

    private static final String CUSTOM_MAIL_MESSAGE_TYPE = "CustomMailMessage";

    @Inject
    protected CoreSession session;

    @Test
    public void testBlobHolder() throws Exception {
        DocumentModel mail1 = session.createDocumentModel("/", "mail1", MailCoreConstants.MAIL_MESSAGE_TYPE);
        mail1.setPropertyValue(MailCoreConstants.HTML_TEXT_PROPERTY_NAME, "foo");
        mail1 = session.createDocument(mail1);
        DocumentModel mail2 = session.createDocumentModel("/", "mail2", MailCoreConstants.MAIL_MESSAGE_TYPE);
        mail2.setPropertyValue(MailCoreConstants.HTML_TEXT_PROPERTY_NAME, "bar");
        mail2 = session.createDocument(mail2);
        session.save();

        BlobHolder bh1 = mail1.getAdapter(BlobHolder.class);
        assertNotNull(bh1);
        Blob blob1 = bh1.getBlob();
        assertNotNull(blob1);
        BlobHolder bh2 = mail2.getAdapter(BlobHolder.class);
        assertNotNull(bh2);
        Blob blob2 = bh2.getBlob();
        assertNotNull(blob2);
        assertNotEquals("Digest of blobs should be different", blob1.getDigest(), blob2.getDigest());
    }

    /**
     * 
     * @since 2021.13
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.mail.test:OSGI-INF/nxmail-core-contrib.xml")
    public void testMailMessageBlobHolderFactory() {
        DocumentModel mailMessage = session.createDocumentModel(MailCoreConstants.MAIL_MESSAGE_TYPE);
        BlobHolder adapter = mailMessage.getAdapter(BlobHolder.class);
        assertTrue(adapter instanceof MailMessageBlobHolder);

        DocumentModel customMailMessage = session.createDocumentModel(CUSTOM_MAIL_MESSAGE_TYPE);
        assertTrue(customMailMessage.getFacets().contains(MailCoreConstants.MAIL_MESSAGE_FACET));
        adapter = customMailMessage.getAdapter(BlobHolder.class);
        assertTrue(adapter instanceof MailMessageBlobHolder);
    }
}
