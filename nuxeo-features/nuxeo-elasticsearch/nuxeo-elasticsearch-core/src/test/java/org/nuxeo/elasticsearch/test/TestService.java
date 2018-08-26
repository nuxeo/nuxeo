/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */

package org.nuxeo.elasticsearch.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Features({ RepositoryElasticSearchFeature.class })
public class TestService {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Inject
    ElasticSearchAdmin esa;

    @Inject
    ElasticSearchService ess;

    @Inject
    ElasticSearchIndexing esi;

    @Test
    public void checkDeclaredServices() throws Exception {
        Assert.assertNotNull(ess);
        Assert.assertNotNull(esi);
        Assert.assertNotNull(esa);

        ESClient client = esa.getClient();
        Assert.assertNotNull(client);

        Assert.assertEquals(0, esa.getTotalCommandProcessed());
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        Assert.assertEquals(0, esa.getRunningWorkerCount());
        Assert.assertFalse(esa.isIndexingInProgress());
        Assert.assertEquals(1, esa.getRepositoryNames().size());
        Assert.assertEquals("test", esa.getRepositoryNames().get(0));
    }

    @Test
    public void verifyNodeStartedWithConfig() throws Exception {

        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        Assert.assertNotNull(esa);

        Assert.assertTrue(esa.getClient().waitForYellowStatus(null, 10));
    }

    @Test
    public void verifyPrepareWaitForIndexing() throws Exception {
        ListenableFuture<Boolean> futureRet = esa.prepareWaitForIndexing();
        Assert.assertFalse(futureRet.isCancelled());
        Assert.assertTrue(futureRet.get());
        Assert.assertTrue(futureRet.isDone());
        Assert.assertTrue(futureRet.get());
    }

    @Test
    public void verifyPrepareWaitForIndexingTimeout() throws Exception {
        // when a worker is created it is pending
        Assert.assertFalse(esa.isIndexingInProgress());
        esi.runReindexingWorker("test", "select * from Document");
        ListenableFuture<Boolean> futureRet = esa.prepareWaitForIndexing();
        try {
            futureRet.get(0, TimeUnit.MILLISECONDS);
            // sometime we don't timeout
            Assert.assertTrue(futureRet.isDone());
        } catch (TimeoutException e) {
            Assert.assertTrue(futureRet.get());
        } finally {
            Assert.assertFalse(esa.isIndexingInProgress());
        }
    }

    @Test
    public void verifyPrepareWaitForIndexingListener() throws Exception {
        ListenableFuture<Boolean> futureRet = esa.prepareWaitForIndexing();
        final Boolean[] callbackRet = { false };
        Futures.addCallback(futureRet, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                callbackRet[0] = true;
                // System.out.println("Success");
            }

            @Override
            public void onFailure(Throwable t) {
                Assert.fail("Fail");
            }
        }, MoreExecutors.newDirectExecutorService());

        Assert.assertTrue(futureRet.get());
        // callback are executed in async, :/
        Thread.sleep(200);
        Assert.assertTrue(callbackRet[0]);
    }

}
