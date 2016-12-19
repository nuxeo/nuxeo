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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.audit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestRegisterAuditAction extends NXRuntimeTestCase {

    ActionService as;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.audit.web.tests", "actions-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.audit.web.tests", "nxauditclient-bundle.xml");
        as = (ActionService) runtime.getComponent(ActionService.ID);
    }

    @Test
    public void testRegistration() {
        Action act1 = as.getAction("TAB_CONTENT_HISTORY");

        assertEquals("action.view.history", act1.getLabel());
        assertEquals("/icons/file.gif", act1.getIcon());
        assertEquals("/incl/tabs/document_history.xhtml", act1.getLink());
        assertTrue(act1.isEnabled());
    }

}
