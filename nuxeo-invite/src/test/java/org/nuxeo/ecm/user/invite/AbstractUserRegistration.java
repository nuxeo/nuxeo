package org.nuxeo.ecm.user.invite;

import com.google.inject.Inject;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.user.invite" })
@RepositoryConfig(type = BackendType.H2, init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
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
