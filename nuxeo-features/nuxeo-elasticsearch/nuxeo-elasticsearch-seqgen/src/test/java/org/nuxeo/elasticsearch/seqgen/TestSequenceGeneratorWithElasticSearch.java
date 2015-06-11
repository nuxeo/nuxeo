/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Tiry
 */
package org.nuxeo.elasticsearch.seqgen;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.uidgen.service.UIDGeneratorService;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.uidgen.core", "org.nuxeo.elasticsearch.seqgen" })
@LocalDeploy({ "org.nuxeo.elasticsearch.seqgen:elasticsearch-seqgen-test-contrib.xml" })
public class TestSequenceGeneratorWithElasticSearch {

    @Inject
    protected UIDGeneratorService uidGeneratorService;

    @Test
    public void testIncrement() throws Exception {
        UIDSequencer seq = uidGeneratorService.getSequencer(ESUIDSequencer.SEQUENCER_CONTRIB);
        Assert.assertNotNull(seq);
        Assert.assertTrue(seq.getClass().isAssignableFrom(ESUIDSequencer.class));

        Assert.assertEquals(1, seq.getNext("myseq"));
        Assert.assertEquals(2, seq.getNext("myseq"));
        Assert.assertEquals(3, seq.getNext("myseq"));
        Assert.assertEquals(1, seq.getNext("myseq2"));
        Assert.assertEquals(4, seq.getNext("myseq"));
        Assert.assertEquals(2, seq.getNext("myseq2"));
    }

    @Test
    public void testConcurrency() throws Exception {
        final String seqName = "mt";
        int nbCalls = 5000;

        final UIDSequencer seq = uidGeneratorService.getSequencer(ESUIDSequencer.SEQUENCER_CONTRIB);
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(nbCalls + 1));

        for (int i = 0; i < nbCalls; i++) {
            tpe.submit(new Runnable() {
                @Override
                public void run() {
                    seq.getNext(seqName);
                }
            });
        }

        tpe.shutdown();
        boolean finish = tpe.awaitTermination(20, TimeUnit.SECONDS);
        Assert.assertTrue("timeout", finish);

        Assert.assertEquals(nbCalls + 1, seq.getNext(seqName));
    }

}
