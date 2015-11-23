package org.nuxeo.ecm.user.center.dashboard;

import static org.nuxeo.ecm.user.center.dashboard.AnonymousDashboardSpaceCreator.ANONYMOUS_DASHBOARD_SPACE_NAME;
import static org.nuxeo.ecm.user.center.dashboard.AnonymousDashboardSpaceCreator.DASHBOARD_MANAGEMENT_PATH;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.spaces.api.AbstractSpaceProvider;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;

/**
 * Space provider for Anonymous user dashboard.
 * <p>
 * Used when administrating the dashboard and when an anonymous user requests
 * its dashboard.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AnonymousDashboardSpaceProvider extends AbstractSpaceProvider {

    private static final Log log = LogFactory.getLog(AnonymousDashboardSpaceProvider.class);

    @Override
    protected Space doGetSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName,
            Map<String, String> parameters) throws SpaceException {
        try {
            return getOrCreateSpace(session, parameters);
        } catch (ClientException e) {
            log.error("Unable to create or get anonymous dashboard", e);
            return null;
        }
    }

    protected Space getOrCreateSpace(CoreSession session,
            Map<String, String> parameters) throws ClientException {
        DocumentRef spaceRef = new PathRef(DASHBOARD_MANAGEMENT_PATH,
                ANONYMOUS_DASHBOARD_SPACE_NAME);
        if (session.exists(spaceRef)) {
            DocumentModel existingSpace = session.getDocument(spaceRef);
            return existingSpace.getAdapter(Space.class);
        } else {
            DocumentRef anonymousDashboardSpaceRef = getOrCreateAnonymousDashboardSpace(
                    session, parameters);
            DocumentModel anonymousDashboardSpace = session.getDocument(anonymousDashboardSpaceRef);
            return anonymousDashboardSpace.getAdapter(Space.class);
        }
    }

    protected DocumentRef getOrCreateAnonymousDashboardSpace(
            CoreSession session, Map<String, String> parameters)
            throws ClientException {
        AnonymousDashboardSpaceCreator anonymousDashboardSpaceCreator = new AnonymousDashboardSpaceCreator(
                session, parameters);
        anonymousDashboardSpaceCreator.runUnrestricted();
        return anonymousDashboardSpaceCreator.anonymousDashboardSpaceRef;
    }

    @Override
    public boolean isReadOnly(CoreSession session) {
        return true;
    }

}
