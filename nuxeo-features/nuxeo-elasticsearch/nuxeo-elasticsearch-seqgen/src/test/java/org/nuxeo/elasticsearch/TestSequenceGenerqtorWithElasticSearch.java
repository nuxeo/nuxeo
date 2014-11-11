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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.seqgen.ESSequenceGeneratorComponent;
import org.nuxeo.elasticsearch.seqgen.SequenceGenerator;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@Deploy({ "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit",
        "org.nuxeo.elasticsearch.seqgen" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.seqgen:elasticsearch-test-contrib.xml" })
public class TestSequenceGenerqtorWithElasticSearch {

    protected @Inject
    CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Test
    public void shouldUseESBackend() throws Exception {
        SequenceGenerator sg = Framework.getService(SequenceGenerator.class);
        Assert.assertNotNull(sg);
        Assert.assertTrue(sg instanceof ESSequenceGeneratorComponent);
    }

    protected void flushAndSync() throws Exception {

        TransactionHelper.commitOrRollbackTransaction();

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        esa.getClient().admin().indices().prepareFlush(
                ESSequenceGeneratorComponent.IDX_NAME).execute().actionGet();
        esa.getClient().admin().indices().prepareRefresh(
                ESSequenceGeneratorComponent.IDX_NAME).execute().actionGet();

        TransactionHelper.startTransaction();
    }

    @Test
    public void testIncrement() throws Exception {

        SequenceGenerator sg = Framework.getService(SequenceGenerator.class);

        Assert.assertEquals(1, sg.getNextId("myseq"));
        Assert.assertEquals(2, sg.getNextId("myseq"));
        Assert.assertEquals(3, sg.getNextId("myseq"));
        Assert.assertEquals(1, sg.getNextId("myseq2"));
        Assert.assertEquals(4, sg.getNextId("myseq"));
        Assert.assertEquals(2, sg.getNextId("myseq2"));

    }

    @Test
    public void testConcurrency() throws Exception {

        final String seqName = "mt";
        int nbCalls = 5000;

        final SequenceGenerator sg = Framework.getService(SequenceGenerator.class);
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(
                        nbCalls + 1));

        for (int i = 0; i < nbCalls; i++) {
            tpe.submit(new Runnable() {
                @Override
                public void run() {
                    sg.getNextId(seqName);
                }
            });
        }

        tpe.shutdown();
        boolean finish = tpe.awaitTermination(5, TimeUnit.SECONDS);
        Assert.assertTrue(finish);

        Assert.assertEquals(nbCalls + 1, sg.getNextId(seqName));

    }

}
