/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.picture.core.test;

import static org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction.ACTION_NAME;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.AbstractTestBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction;
import org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsActionValidation;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestRecomputeViewsActionValidation
extends AbstractTestBulkActionValidation<RecomputeViewsActionValidation> {

    public TestRecomputeViewsActionValidation() {
        super(RecomputeViewsActionValidation.class);
    }

    @Test
    public void testInvalidParams() {
        String query = "SELECT * FROM Document";
        BulkCommand command = createBuilder(ACTION_NAME, query, "test", "test")
                .param(RecomputeViewsAction.PARAM_XPATH, "fake:xpath")
                .build();
        assertInvalidCommand(command, "Unknown xpath fake:xpath in command: " + command);
    }
}
