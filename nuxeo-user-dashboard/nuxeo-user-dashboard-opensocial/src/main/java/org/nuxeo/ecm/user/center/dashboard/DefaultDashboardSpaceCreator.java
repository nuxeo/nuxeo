package org.nuxeo.ecm.user.center.dashboard;

import static org.nuxeo.ecm.spaces.api.Constants.SPACE_DOCUMENT_TYPE;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Create the default dashboard {@code Space} in an Unrestricted Session.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DefaultDashboardSpaceCreator extends AbstractDashboardSpaceCreator {

    public static final String DEFAULT_DASHBOARD_SPACE_NAME = "defaultDashboardSpace";

    public static final String POWER_USERS_GROUP = "powerusers";

    private static final Log log = LogFactory.getLog(DefaultDashboardSpaceCreator.class);

    public DocumentRef defaultDashboardSpaceRef;

    public DefaultDashboardSpaceCreator(CoreSession session,
            Map<String, String> parameters) {
        super(session, parameters);
    }

    @Override
    public void run() throws ClientException {
        DocumentModel dashboardManagement = getDashboardManagement();
        String defaultDashboardSpacePath = new Path(
                dashboardManagement.getPathAsString()).append(
                DEFAULT_DASHBOARD_SPACE_NAME).toString();
        DocumentRef defaultDashboardSpacePathRef = new PathRef(
                defaultDashboardSpacePath);

        DocumentModel defaultDashboardSpace;
        if (!session.exists(defaultDashboardSpacePathRef)) {
            defaultDashboardSpace = createDefaultDashboardSpace(dashboardManagement.getPathAsString());
        } else {
            defaultDashboardSpace = session.getDocument(defaultDashboardSpacePathRef);
        }
        defaultDashboardSpaceRef = defaultDashboardSpace.getRef();
    }

    protected DocumentModel createDefaultDashboardSpace(
            String dashboardManagementPath) throws ClientException {
        DocumentModel defaultDashboardSpace = session.createDocumentModel(
                dashboardManagementPath, DEFAULT_DASHBOARD_SPACE_NAME,
                SPACE_DOCUMENT_TYPE);
        defaultDashboardSpace.setPropertyValue("dc:title",
                "default dashboard space");
        defaultDashboardSpace.setPropertyValue("dc:description",
                "default dashboard space");
        defaultDashboardSpace = session.createDocument(defaultDashboardSpace);

        addInitialGadgets(defaultDashboardSpace);
        addDefaultACP(defaultDashboardSpace);
        return session.saveDocument(defaultDashboardSpace);
    }

    protected void addDefaultACP(DocumentModel defaultDashboardSpace)
            throws ClientException {
        ACP acp = defaultDashboardSpace.getACP();
        ACL acl = acp.getOrCreateACL();
        for (String group : getUserManager().getAdministratorsGroups()) {
            acl.add(new ACE(group, SecurityConstants.EVERYTHING, true));
        }
        acl.add(new ACE(POWER_USERS_GROUP, SecurityConstants.EVERYTHING, true));
        defaultDashboardSpace.setACP(acp, true);
    }

}
