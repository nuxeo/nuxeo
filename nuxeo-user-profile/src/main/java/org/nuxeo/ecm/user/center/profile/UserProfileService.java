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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * A service to manage user profiles
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.5
 */
public interface UserProfileService {

    /**
     * Get the current user profile. It's stored in its user workspace.
     *
     * @param the current CoreSession
     * @return the user profile DocumentModel
     */
    DocumentModel getUserProfileDocument(CoreSession session);

    /**
     * Get the profile of a specific user. It's stored in its user workspace.
     *
     * @param the user name
     * @param the current CoreSession
     * @return the user profile DocumentModel
     */
    DocumentModel getUserProfileDocument(String userName, CoreSession session);

    /**
     * Get a DocumentModel containing both user and user profile schemas
     *
     * @param the user DocumentModel
     * @param the current CoreSession
     * @return a SimpleDocumentModel with the two schemas
     */
    DocumentModel getUserProfile(DocumentModel userModel, CoreSession session);

    /**
     * Clears the user profile cache.
     */
    void clearCache();

    /**
     * Get the user profile XMap descriptor for the importer config
     *
     * @return an XMap ImportConfig descriptor
     */
    ImporterConfig getImporterConfig();
}
