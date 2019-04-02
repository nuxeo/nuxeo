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
 *     Salem Aouana
 */

package org.nuxeo.ecm.imaging.recompute.lang.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.actions.jsf.JSFActionContext;
import org.nuxeo.ecm.platform.ui.web.jsf.MockFacesContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.actions", "org.nuxeo.ecm.webapp.core:OSGI-INF/actions-contrib.xml",
        "org.nuxeo.ecm.platform.picture.core:OSGI-INF/picture-schemas-contrib.xml",
        "org.nuxeo.ecm.platform.picture.jsf" })
public class PerformingImageActionTest {

    public static final String USER_WITH_READ_PERMISSION = "robert";

    public static final String USER_WITH_READ_WRITE_PERMISSION = "james";

    public static final String USER_WITH_EVERYTHING_PERMISSION = "anotherAdmin";

    public static final String ROTATE_PICTURE_FILTER_NAME = "canRotatePicture";

    @Inject
    protected ActionManager actionService;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected CoreFeature coreFeature;

    protected MockFacesContext facesContext;

    protected DocumentRef documentRef;

    @Before
    public void initRepository() {
        facesContext = new MockFacesContext();
        facesContext.setCurrent();

        DocumentModel document = coreSession.createDocumentModel("/", "anyPicture", "Picture");
        document = coreSession.createDocument(document);

        ACE[] aces = { getACE(USER_WITH_READ_PERMISSION, SecurityConstants.READ),
                getACE(USER_WITH_READ_WRITE_PERMISSION, SecurityConstants.READ_WRITE),
                getACE(USER_WITH_EVERYTHING_PERMISSION, SecurityConstants.EVERYTHING) };

        ACP acp = document.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.setACEs(aces);
        document.setACP(acp, true);

        documentRef = document.getRef();
        coreSession.save();
    }

    @Test
    public void shouldBeRotatableByTheCreator() {
        assertTrue(actionService.checkFilter(ROTATE_PICTURE_FILTER_NAME, getActionContext(coreSession)));
    }

    @Test
    public void shouldNotBeRotatableByUnauthorizedUser() {
        checkFilter(USER_WITH_READ_PERMISSION, false);
    }

    @Test
    public void shouldBeRotatableByAuthorizedUser() {
        checkFilter(USER_WITH_READ_WRITE_PERMISSION, true);
    }

    @Test
    public void shouldBeRotatableBySuperUser() {
        checkFilter(USER_WITH_EVERYTHING_PERMISSION, true);
    }

    protected ActionContext getActionContext(CoreSession session) {
        ActionContext context = new JSFActionContext(facesContext);
        context.setCurrentDocument(session.getDocument(documentRef));
        context.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());
        context.setDocumentManager(session);
        return context;
    }

    protected ACE getACE(String username, String permission) {
        return new ACE(username, permission, true);
    }

    protected void checkFilter(String username, boolean expected) {
        CoreSession session = null;
        try {
            session = coreFeature.openCoreSession(username);
            assertEquals(expected, actionService.checkFilter(ROTATE_PICTURE_FILTER_NAME, getActionContext(session)));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
