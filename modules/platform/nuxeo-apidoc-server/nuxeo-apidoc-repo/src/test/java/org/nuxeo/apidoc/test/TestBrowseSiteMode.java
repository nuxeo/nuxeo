/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.2
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestBrowseSiteMode {

    @Inject
    protected SnapshotManager manager;

    @Inject
    protected CoreSession session;

    @Before
    public void setSiteMode() {
        Framework.getRuntime().setProperty(SnapshotManager.PROPERTY_SITE_MODE, "true");
    }

    @Test
    public void testIntrospection() {
        assertTrue(manager.isSiteMode());
        try {
            manager.getRuntimeSnapshot();
            fail("Should have raised a runtime exception");
        } catch (RuntimeServiceException e) {
            // ok
        }
    }

    @Test
    public void testGetSnapshot() {
        assertTrue(manager.isSiteMode());
        try {
            manager.getSnapshot(SnapshotManager.DISTRIBUTION_ALIAS_CURRENT, session);
            fail("Should have raised a runtime exception");
        } catch (RuntimeServiceException e) {
            // ok
        }
        try {
            manager.getSnapshot(SnapshotManager.DISTRIBUTION_ALIAS_ADM, session);
            fail("Should have raised a runtime exception");
        } catch (RuntimeServiceException e) {
            // ok
        }
    }

}
