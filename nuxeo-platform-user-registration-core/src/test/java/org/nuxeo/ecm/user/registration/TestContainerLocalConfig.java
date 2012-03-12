package org.nuxeo.ecm.user.registration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.user.registration" })
@LocalDeploy({ "org.nuxeo.ecm.user.registration:test-types-contrib.xml" })
public class TestContainerLocalConfig {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserRegistrationService userRegistrationService;

    @Test
    public void testGetRegistrationRules() throws ClientException {
        RegistrationRules rules = userRegistrationService.getRegistrationRules();
        assertNotNull(rules);

        assertTrue(rules.allowUserCreation());
        session.save();

        DocumentModel root = ((UserRegistrationComponent)userRegistrationService).getOrCreateRootDocument(session);
        root.setPropertyValue(RegistrationRules.FIELD_ALLOW_USER_CREATION, false);
        session.saveDocument(root);
        session.save();

        rules = userRegistrationService.getRegistrationRules();
        assertFalse(rules.allowUserCreation());
    }
}
