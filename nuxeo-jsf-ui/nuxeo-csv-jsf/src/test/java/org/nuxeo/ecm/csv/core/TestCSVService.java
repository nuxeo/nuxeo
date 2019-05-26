/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ecm.csv.core;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.csv.core")
@Deploy("org.nuxeo.ecm.csv.jsf")
public class TestCSVService {

    @Inject
    private CSVImporter mImporter;

    @Inject
    private CoreSession mSession;

    @Inject
    private ActionService mActionService;

    @Test
    public void testShouldHasAccessToService() {
        assertNotNull(mImporter);
        assertNotNull(mSession);
        assertNotNull(mActionService);

        // CSVImportActions actions = Mockito.mock(CSVImportActions.class);
        // assertNotNull(actions);
        // mActionService.activate((ComponentContext) actions);
    }
}
