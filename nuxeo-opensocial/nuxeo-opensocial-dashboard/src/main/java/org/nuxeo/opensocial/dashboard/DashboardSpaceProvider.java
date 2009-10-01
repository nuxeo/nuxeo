package org.nuxeo.opensocial.dashboard;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.spaces.core.contribs.impl.DefaultSpaceProvider;
import org.nuxeo.ecm.spaces.core.impl.Constants;
import org.nuxeo.ecm.spaces.core.impl.DocumentHelper;
import org.nuxeo.ecm.spaces.core.impl.exceptions.NoElementFoundException;

public class DashboardSpaceProvider extends DefaultSpaceProvider {

    public static final String DASHBOARD_SPACE_NAME = "dashboardSpace";

    @Override
    public Space create(Space data, Univers parent, CoreSession session)
            throws SpaceException {

        throw new SpaceException("Cannot create other spaces with the "
                + "dashboard space provider!");
    }

    @Override
    public Space getElement(String name, Univers parent, CoreSession session)
            throws NoElementFoundException, SpaceSecurityException {
        if (!name.equals(DASHBOARD_SPACE_NAME)) {
            throw new SpaceSecurityException(
                    "Only one space is supported by the "
                            + "dashboard space provider!");
        }
        try {
            IdRef universeRef = new IdRef(parent.getId());
            DocumentModel universeDoc = session.getDocument(universeRef);
            PathRef spaceRef = new PathRef(universeDoc.getPathAsString() + "/"
                    + DASHBOARD_SPACE_NAME);
            DocumentModel spaceDocument;
            if (session.exists(spaceRef)) {
                spaceDocument = session.getDocument(spaceRef);
            } else {
                DocumentModel model = session.createDocumentModel(
                        universeDoc.getPathAsString(), DASHBOARD_SPACE_NAME,
                        Constants.Space.TYPE);
                model.setProperty("dc", "title", "nuxeo dashboard space");
                model.setProperty("dc", "description", "dashboard space");
                Space desiredSpace = model.getAdapter(Space.class);
                spaceDocument = DocumentHelper.createInternalDocument(
                        universeDoc, desiredSpace.getName(),
                        desiredSpace.getTitle(), desiredSpace.getDescription(),
                        session, Constants.Space.TYPE);
            }
            return spaceDocument.getAdapter(Space.class);
        } catch (ClientException e) {
            // really not ideal
            throw new SpaceSecurityException(e);
        }
    }
}
