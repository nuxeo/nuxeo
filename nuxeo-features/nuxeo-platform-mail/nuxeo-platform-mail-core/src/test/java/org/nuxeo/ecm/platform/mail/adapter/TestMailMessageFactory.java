/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */
package org.nuxeo.ecm.platform.mail.adapter;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * 
 * @since 10.10-HF55
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.mail.types")
@Deploy("org.nuxeo.ecm.platform.mail:OSGI-INF/nxmail-blobholder-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.mail.test:OSGI-INF/nxmail-core-contrib.xml")
public class TestMailMessageFactory {

    private static final String CUSTOM_MAIL_MESSAGE_TYPE = "CustomMailMessage";

    @Inject
    protected CoreSession session;

    @Test
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
