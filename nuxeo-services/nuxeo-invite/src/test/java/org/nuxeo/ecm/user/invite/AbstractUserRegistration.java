/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 */
package org.nuxeo.ecm.user.invite;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.user.invite" })
@RepositoryConfig(init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.user.invite:test-types-contrib.xml" })
public abstract class AbstractUserRegistration {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected UserInvitationService userRegistrationService;

    public void initializeRegistrations() throws ClientException {
        DocumentModel container = session.createDocumentModel("Workspace");
        container.setPathInfo("/", "requests");
        session.createDocument(container);

        container = session.createDocumentModel("Workspace");
        container.setPathInfo("/", "test-requests");
        session.createDocument(container);

        session.save();

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }
}
