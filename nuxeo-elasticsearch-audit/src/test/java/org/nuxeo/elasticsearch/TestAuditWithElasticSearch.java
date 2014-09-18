/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Tiry
 * 
 */
package org.nuxeo.elasticsearch;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@Deploy({ "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit",
        "org.nuxeo.elasticsearch.audit" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.audit:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:audit-test-contrib.xml" })
public class TestAuditWithElasticSearch {

    protected @Inject
    CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Test
    public void shouldUseESBackend() throws Exception {

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
        Assert.assertNotNull(audit);

        AuditBackend backend = audit.getBackend();
        Assert.assertNotNull(backend);

        Assert.assertTrue(backend instanceof ESAuditBackend);
    }

    protected void flushAndSync() throws Exception {
        
        TransactionHelper.commitOrRollbackTransaction();
        
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        esa.getClient().admin().indices()
        .prepareFlush(ESAuditBackend.IDX_NAME).execute()
        .actionGet();
        esa.getClient().admin().indices()
        .prepareRefresh(ESAuditBackend.IDX_NAME).execute()
        .actionGet();

        TransactionHelper.startTransaction();

    }
    
    @Test
    public void shouldLogInAudit() throws Exception {
        // generate events
        DocumentModel doc = session.createDocumentModel("/", "a-file", "File");
        doc.setPropertyValue("dc:title", "A File");
        doc = session.createDocument(doc);
        
        //doc.setPropertyValue("dc:title", "A modified File");
        //doc = session.saveDocument(doc);

        flushAndSync();
        
        // test audit trail
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId());

        assertThat(trail, notNullValue());

        Assert.assertEquals(1, trail.size());        
        Assert.assertEquals(0,trail.get(0).getId());
        Assert.assertEquals("documentCreated",trail.get(0).getEventId());
        Assert.assertEquals("eventDocumentCategory",trail.get(0).getCategory());
        Assert.assertEquals("A File",trail.get(0).getExtendedInfos().get("title").getValue(String.class));        
    }
    
    

}
