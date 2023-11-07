/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertThrows;
import static org.nuxeo.ecm.core.blob.scroll.RepositoryBlobScroll.SCROLL_NAME;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2021.5
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, CoreBulkFeature.class })
public abstract class AbstractTestUnsupportedFullGCOrphanBlobs {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected BulkService service;

    @Inject
    protected CoreSession session;

    protected void assertdoGCNotImplemented() {
        assertThrows(NuxeoException.class, () -> {
            BulkCommand command = new BulkCommand.Builder(GarbageCollectOrphanBlobsAction.ACTION_NAME,
                    session.getRepositoryName(), session.getPrincipal().getName()).repository(
                            session.getRepositoryName()).useGenericScroller().scroller(SCROLL_NAME).build();
            service.submit(command);
        });
    }
}
