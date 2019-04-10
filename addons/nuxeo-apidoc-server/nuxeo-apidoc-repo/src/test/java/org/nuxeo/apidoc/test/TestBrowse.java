/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestBrowse {

    protected SnapshotManager getSnapshotManager() {
        return Framework.getService(SnapshotManager.class);
    }

    @Inject
    SnapshotManager manager;

    @Test
    public void testIntrospection() {

        String cid = "org.nuxeo.ecm.core.lifecycle.LifeCycleService";
        DistributionSnapshot runtimeSnapshot = getSnapshotManager().getRuntimeSnapshot();

        ComponentInfo ci = runtimeSnapshot.getComponent(cid);
        assertNotNull(ci);

        assertEquals(2, ci.getExtensionPoints().size());

        String epid = "org.nuxeo.ecm.core.lifecycle.LifeCycleService--types";

        ExtensionPointInfo epi = runtimeSnapshot.getExtensionPoint(epid);
        assertNotNull(epi);

        Collection<ExtensionInfo> contribs = epi.getExtensions();
        assertFalse(contribs.isEmpty());
    }

}
