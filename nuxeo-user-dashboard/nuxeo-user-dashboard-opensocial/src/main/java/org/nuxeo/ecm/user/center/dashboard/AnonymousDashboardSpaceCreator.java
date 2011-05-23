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
 * Create the anonymous dashboard {@code Space} in an Unrestricted Session.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AnonymousDashboardSpaceCreator extends
        AbstractDashboardSpaceCreator {

    public static final String ANONYMOUS_DASHBOARD_SPACE_NAME = "anonymousDashboardSpace";

    private static final Log log = LogFactory.getLog(AnonymousDashboardSpaceCreator.class);

    public DocumentRef anonymousDashboardSpaceRef;

    public AnonymousDashboardSpaceCreator(CoreSession session,
            Map<String, String> parameters) {
        super(session, parameters);
    }

    @Override
    public void run() throws ClientException {
        DocumentModel dashboardManagement = getDashboardManagement();
        String anonymousDashboardSpacePath = new Path(
                dashboardManagement.getPathAsString()).append(
                ANONYMOUS_DASHBOARD_SPACE_NAME).toString();
        DocumentRef anonymousDashboardSpacePathRef = new PathRef(
                anonymousDashboardSpacePath);

        DocumentModel anonymousDashboardSpace;
        if (!session.exists(anonymousDashboardSpacePathRef)) {
            anonymousDashboardSpace = createAnonymousDashboardSpace(dashboardManagement.getPathAsString());
        } else {
            anonymousDashboardSpace = session.getDocument(anonymousDashboardSpacePathRef);
        }
        anonymousDashboardSpaceRef = anonymousDashboardSpace.getRef();
    }

    protected DocumentModel createAnonymousDashboardSpace(
            String dashboardManagementPath) throws ClientException {
        DocumentModel anonymousDashboardSpace = session.createDocumentModel(
                dashboardManagementPath, ANONYMOUS_DASHBOARD_SPACE_NAME,
                SPACE_DOCUMENT_TYPE);
        anonymousDashboardSpace.setPropertyValue("dc:title",
                "anonymous dashboard space");
        anonymousDashboardSpace.setPropertyValue("dc:description",
                "anonymous dashboard space");
        anonymousDashboardSpace = session.createDocument(anonymousDashboardSpace);

        addInitialGadgets(anonymousDashboardSpace);
        addAnonymousACP(anonymousDashboardSpace);
        return session.saveDocument(anonymousDashboardSpace);
    }

    protected void addAnonymousACP(DocumentModel anonymousDashboardSpace)
            throws ClientException {
        ACP acp = anonymousDashboardSpace.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(getUserManager().getAnonymousUserId(),
                SecurityConstants.READ, true));
        anonymousDashboardSpace.setACP(acp, true);

    }

}
