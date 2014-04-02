package org.nuxeo.ecm.user.invite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.user.invite.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;

import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class TestContainerLocalConfig extends AbstractUserRegistration {

    @Test
    public void testGetRegistrationRules() throws ClientException {
        initializeRegistrations();

        RegistrationRules rules = userRegistrationService.getRegistrationRules(DEFAULT_CONFIGURATION_NAME);
        assertNotNull(rules);

        assertTrue(rules.allowUserCreation());
        session.save();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        DocumentModel root = ((UserInvitationComponent) userRegistrationService).getOrCreateRootDocument(
                session, DEFAULT_CONFIGURATION_NAME);
        root.setPropertyValue(RegistrationRules.FIELD_ALLOW_USER_CREATION,
                false);
        session.saveDocument(root);
        session.save();

        rules = userRegistrationService.getRegistrationRules(DEFAULT_CONFIGURATION_NAME);
        assertFalse(rules.allowUserCreation());
    }
}
