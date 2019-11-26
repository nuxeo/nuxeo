/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.elasticsearch.test.scroll;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.scroll.TestRepositoryScroll;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryLightElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryLightElasticSearchFeature.class })
public class TestElasticSearchScroll extends TestRepositoryScroll {

    @Override
    public String getType() {
        return "elastic";
    }

    protected void nextTransaction() throws Exception {
        super.nextTransaction();
        waitForIndexing();
    }

    public void waitForIndexing() throws Exception {
        WorkManager wm = Framework.getService(org.nuxeo.ecm.core.work.api.WorkManager.class);
        assertTrue(wm.awaitCompletion(60, TimeUnit.SECONDS));
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

}
