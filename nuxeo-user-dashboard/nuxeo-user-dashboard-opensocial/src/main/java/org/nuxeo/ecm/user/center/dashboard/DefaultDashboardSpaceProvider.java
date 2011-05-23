package org.nuxeo.ecm.user.center.dashboard;

import static org.nuxeo.ecm.user.center.dashboard.AbstractDashboardSpaceCreator.DASHBOARD_MANAGEMENT_PATH;
import static org.nuxeo.ecm.user.center.dashboard.DefaultDashboardSpaceCreator.DEFAULT_DASHBOARD_SPACE_NAME;

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
 * Dashboard space provider returning the default dashboard to be configured by
 * the Administrator.
 * <p>
 * The default dashboard is used when initializing the user dashboard.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DefaultDashboardSpaceProvider extends AbstractSpaceProvider {

    private static final Log log = LogFactory.getLog(DefaultDashboardSpaceProvider.class);

    @Override
    protected Space doGetSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName,
            Map<String, String> parameters) throws SpaceException {
        try {
            return getOrCreateSpace(session, parameters);
        } catch (ClientException e) {
            log.error("Unable to create or get default dashboard", e);
            return null;
        }
    }

    protected Space getOrCreateSpace(CoreSession session,
            Map<String, String> parameters) throws ClientException {
        DocumentRef spaceRef = new PathRef(DASHBOARD_MANAGEMENT_PATH,
                DEFAULT_DASHBOARD_SPACE_NAME);
        if (session.exists(spaceRef)) {
            DocumentModel existingSpace = session.getDocument(spaceRef);
            return existingSpace.getAdapter(Space.class);
        } else {
            DocumentRef defaultDashboardSpaceRef = getOrCreateDefaultDashboardSpace(
                    session, parameters);
            DocumentModel defaultDashboardSpace = session.getDocument(defaultDashboardSpaceRef);
            return defaultDashboardSpace.getAdapter(Space.class);
        }
    }

    protected DocumentRef getOrCreateDefaultDashboardSpace(CoreSession session,
            Map<String, String> parameters) throws ClientException {
        DefaultDashboardSpaceCreator defaultDashboardSpaceCreator = new DefaultDashboardSpaceCreator(
                session, parameters);
        defaultDashboardSpaceCreator.runUnrestricted();
        return defaultDashboardSpaceCreator.defaultDashboardSpaceRef;
    }

    @Override
    public boolean isReadOnly(CoreSession session) {
        return true;
    }

}
