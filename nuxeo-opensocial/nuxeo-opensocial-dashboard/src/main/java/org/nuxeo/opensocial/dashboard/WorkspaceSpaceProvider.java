/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.dashboard;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.spaces.api.AbstractSpaceProvider;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.impl.docwrapper.DocSpaceImpl;
import org.nuxeo.opensocial.container.shared.layout.api.LayoutHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class WorkspaceSpaceProvider extends AbstractSpaceProvider {

    public static final String DEFAULT_SPACE_NAME = "defaultSpace";

    @Override
    public boolean isReadOnly(CoreSession session) {
        return true;
    }

    @Override
    protected Space doGetSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName)
            throws SpaceException {
        if (spaceName == null || spaceName.isEmpty()) {
            spaceName = DEFAULT_SPACE_NAME;
        }
        try {
            PathRef spaceRef = new PathRef(contextDocument.getPathAsString(),
                    spaceName);
            if (session.exists(spaceRef)) {
                DocumentModel space = session.getDocument(spaceRef);
                return space.getAdapter(Space.class);
            } else {
                DocumentModel model = session.createDocumentModel(
                        contextDocument.getPathAsString(), spaceName,
                        DocSpaceImpl.TYPE);
                model.setPropertyValue("dc:title", spaceName);
                model = session.createDocument(model);
                session.save();

                Space space = model.getAdapter(Space.class);
                space.initLayout(LayoutHelper.buildLayout(LayoutHelper.Preset.X_2_66_33));
                return space;
            }
        } catch (ClientException e) {
            throw new SpaceException(e);
        }
    }

}
