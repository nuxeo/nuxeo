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
 *     Thomas Roger
 */
package org.nuxeo.ecm.platform.picture.core;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction.ACTION_NAME;

import org.nuxeo.ecm.automation.core.AutomationCoreFeature;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * since 11.3
 */
@Features(AutomationCoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.picture.core.tests:OSGI-INF/empty-picture-configuration-contrib.xml")
public class ImagingFeature implements RunnerFeature {
    @Override
    public void initialize(FeaturesRunner runner) {
        // picture views generation is made of two dependant async processes, the generation is done as below:
        // - sync listener org.nuxeo.ecm.platform.picture.listener.PictureChangedListener which prefill picture views
        // if the main blob has changed
        // - async listener org.nuxeo.ecm.platform.picture.listener.PictureViewsGenerationListener which checks if
        // picture views have to be computed, and if so it triggers the recomputeViews Bulk Action
        // - async Bulk Action org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction
        // so we need to first wait for the work to finish and then wait for the bulk action to finish
        var coreBulkFeature = runner.getFeature(CoreBulkFeature.class);
        coreBulkFeature.addBulkCommandWaiterForListener(runner, ACTION_NAME, DOCUMENT_CREATED);
        coreBulkFeature.addBulkCommandWaiterForListener(runner, ACTION_NAME, DOCUMENT_UPDATED);
    }
}
