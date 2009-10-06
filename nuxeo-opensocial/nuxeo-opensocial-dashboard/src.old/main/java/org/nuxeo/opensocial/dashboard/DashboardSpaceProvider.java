/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

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
