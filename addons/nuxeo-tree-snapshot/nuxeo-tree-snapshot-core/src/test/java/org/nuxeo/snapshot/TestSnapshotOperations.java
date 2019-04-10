/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.snapshot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.snapshot.operation.CreateTreeSnapshot;

import java.util.HashMap;

import static org.junit.Assert.*;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@RepositoryConfig(init = PublishRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.snapshot" })
public class TestSnapshotOperations extends AbstractTestSnapshot {

    @Test
    public void testSnapshotOperation() throws Exception {
        buildTree();
        folderB1.addFacet(Snapshotable.FACET);
        session.save();

        AutomationService as = Framework.getService(AutomationService.class);
        assertNotNull(as);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(folderB1);

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("versioning option", VersioningOption.MAJOR.name());
        DocumentModel version = (DocumentModel) as.run(ctx, CreateTreeSnapshot.ID, params);

        assertTrue(version.isVersion());
        assertEquals("1.0", version.getVersionLabel());
    }
}
