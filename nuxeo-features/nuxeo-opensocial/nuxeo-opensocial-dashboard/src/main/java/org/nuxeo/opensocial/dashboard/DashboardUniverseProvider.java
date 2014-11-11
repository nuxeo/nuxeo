package org.nuxeo.opensocial.dashboard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.core.contribs.impl.DefaultUniversProvider;
import org.nuxeo.ecm.spaces.core.impl.Constants;
import org.nuxeo.ecm.spaces.core.impl.DocumentHelper;
import org.nuxeo.runtime.api.Framework;

public class DashboardUniverseProvider extends DefaultUniversProvider {
    private static final Log log = LogFactory.getLog(DashboardUniverseProvider.class);

    public static final String DASHBOARD_UNIVERSE_NAME = "dashboardUniverse";

    @Override
    public Univers create(Univers univers, CoreSession session)
            throws SpaceException {
        throw new SpaceException("Cannot create new universes with the "
                + "dashboard universe provider!");
    }

    @Override
    public Univers getElementByName(String name, CoreSession session)
            throws SpaceException {

        if (!name.equals(DASHBOARD_UNIVERSE_NAME)) {
            return null;
        }
        try {
            UserWorkspaceService svc = Framework.getService(UserWorkspaceService.class);
            if (svc == null) {
                throw new SpaceException(
                        "Can't find the user workspace service!");
            }
            DocumentModel personalWorkspace = svc.getCurrentUserPersonalWorkspace(
                    session, null);
            DocumentModel userWorkspace = svc.getCurrentUserPersonalWorkspace(
                    session, null);
            PathRef universePath = new PathRef(userWorkspace.getPathAsString()
                    + "/" + DASHBOARD_UNIVERSE_NAME);
            DocumentModel universeDoc;
            if (session.exists(universePath)) {
                universeDoc = session.getDocument(universePath);

            } else {
                DocumentModel model = session.createDocumentModel("/",
                        DASHBOARD_UNIVERSE_NAME,
                        org.nuxeo.ecm.spaces.core.impl.Constants.Univers.TYPE);
                model.setProperty("dc", "title", "nuxeo dashboard universe");
                model.setProperty("dc", "description",
                        "parent of dashboard space");
                Univers desiredUniverse = model.getAdapter(Univers.class);

                universeDoc = DocumentHelper.createInternalDocument(
                        personalWorkspace, desiredUniverse.getName(),
                        desiredUniverse.getTitle(),
                        desiredUniverse.getDescription(), session,
                        Constants.Univers.TYPE);
            }
            return universeDoc.getAdapter(Univers.class);
        } catch (Exception e) {
            throw new SpaceException(e);
        }
    }
}
