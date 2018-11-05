/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.trash.test;

import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.trash.PropertyTrashService.SYSPROP_IS_TRASHED;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.trash.PropertyTrashService;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Class to test the backward mechanism when following a delete/undelete transition.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-property-override.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestTrashBackwardMechanism {

    @Inject
    protected CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    @Test
    public void testPropertyTrashService() {
        assertTrue(Framework.getService(TrashService.class) instanceof PropertyTrashService);
    }

    @Test
    public void testMechanism() {
        DocumentModel doc = session.createDocumentModel("/", "file001", "File");
        doc = session.createDocument(doc);
        session.save();

        // follow delete transition to check if TrashService is called
        session.followTransition(doc, LifeCycleConstants.DELETE_TRANSITION);

        coreFeature.waitForAsyncCompletion();

        // we assert ecm:isTrashed property in order to confirm that TrashService has been called
        assertTrue(session.isTrashed(doc.getRef()));
        Boolean isTrashed = session.getDocumentSystemProp(doc.getRef(), SYSPROP_IS_TRASHED, Boolean.class);
        assertTrue(isTrashed != null && isTrashed.booleanValue());
    }

}
