package org.nuxeo.ecm.user.center.dashboard.seam;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.opensocial.helper.OpenSocialGadgetHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("userDashboardActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class UserDashboardActions implements Serializable {

    public static final String USER_DASHBOARD_SPACE_PROVIDER = "userDashboardSpaceProvider";

    @In(create = true)
    protected transient CoreSession documentManager;

    public String removeUserDashboard() throws ClientException {
        SpaceManager spaceManager = getSpaceManager();
        Space userSpace = spaceManager.getSpace(USER_DASHBOARD_SPACE_PROVIDER,
                documentManager);
        if (userSpace != null) {
            DocumentRef spaceRef = new IdRef(userSpace.getId());
            documentManager.removeDocument(spaceRef);
            documentManager.save();
        }
        return null;
    }

    protected SpaceManager getSpaceManager() throws ClientException {
        try {
            return Framework.getService(SpaceManager.class);
        } catch (Exception e) {
            throw new ClientException(
                    "Unable to retrieve SpaceManager service", e);
        }
    }

    @Factory(value = "gadgetsBaseURL", scope = APPLICATION)
    public String getGadgetsBaseURL() {
        return OpenSocialGadgetHelper.getGadgetsBaseUrl(false);
    }

}
