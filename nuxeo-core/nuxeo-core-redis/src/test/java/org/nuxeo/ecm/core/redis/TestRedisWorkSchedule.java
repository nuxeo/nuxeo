package org.nuxeo.ecm.core.redis;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, RedisFeature.class })
public class TestRedisWorkSchedule {

    @Inject
    CoreSession session;

    static TestRedisWorkSchedule self;

    @Before
    public void injectSelf() {
        self = this;
    }

    @After
    public void outjectSelf() {
        self = null;
    }

    final CountDownLatch observed = new CountDownLatch(1);

    public static class PfouhListener implements PostCommitEventListener {

        public PfouhListener() {
            super();
        }

        @Override
        public void handleEvent(EventBundle events) {
            self.observed.countDown();
        }

    }

    static class DocWrapper implements Serializable {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        final DocumentModel doc;

        public DocWrapper(DocumentModel source) {
            doc = source;
        }

    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event:test-redis-pfouh-listener.xml")
    public void canSerializeEventBundles() throws InterruptedException {
        Assert.assertTrue(TransactionHelper.isTransactionActive());
        DocumentModel doc = session.createDocumentModel("/", "pfouh", "Document");
        doc = session.createDocument(doc);
        DocumentEventContext context = new DocumentEventContext(session, session.getPrincipal(), doc);
        context.newEvent("pfouh");
        context.getProperties().put("pfouh", new DocWrapper(doc));
        Framework.getService(EventProducer.class).fireEvent(context.newEvent("pfouh"));
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        Assert.assertTrue(observed.await(10, TimeUnit.SECONDS));
    }
}
