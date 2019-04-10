/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.apidoc.snapshot.DistributionSnapshot.PROP_LATEST_FT;
import static org.nuxeo.apidoc.snapshot.DistributionSnapshot.PROP_LATEST_LTS;
import static org.nuxeo.apidoc.snapshot.DistributionSnapshot.TYPE_NAME;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestLatestDistributionListener {

    @Inject
    CoreSession session;

    @Inject
    TransactionalFeature txFeature;

    @Test
    public void testLatestFlagAtomicity() {
        Arrays.asList(PROP_LATEST_LTS, PROP_LATEST_FT).forEach(this::testDistributionCreation);
    }

    protected void testDistributionCreation(String flag) {
        // Put flag at true at creation time
        DocumentModel dis1 = session.createDocumentModel("/", "distrib1", TYPE_NAME);
        dis1.setPropertyValue(flag, true);
        dis1 = session.createDocument(dis1);
        txFeature.nextTransaction();

        assertTrue((Boolean) dis1.getPropertyValue(flag));

        // Create a new distribution with the flag sets to true
        DocumentModel dis2 = session.createDocumentModel("/", "distrib2", TYPE_NAME);
        dis2.setPropertyValue(flag, true);
        dis2 = session.createDocument(dis2);
        txFeature.nextTransaction();

        assertTrue((Boolean) dis2.getPropertyValue(flag));
        dis1 = session.getDocument(dis1.getRef());
        // Ensure the second one is false
        assertFalse((Boolean) dis1.getPropertyValue(flag));

        // Reset flag on the first distribution
        dis1.setPropertyValue(flag, true);
        session.saveDocument(dis1);
        txFeature.nextTransaction();

        assertFalse((Boolean) session.getDocument(dis2.getRef()).getPropertyValue(flag));
    }
}
