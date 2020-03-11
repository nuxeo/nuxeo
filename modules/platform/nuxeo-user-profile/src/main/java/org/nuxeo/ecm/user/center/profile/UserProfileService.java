/*
 * (C) Copyright 2011-2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile;

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
     * @param session the current CoreSession
     * @return the user profile DocumentModel
     */
    DocumentModel getUserProfileDocument(CoreSession session);

    /**
     * Get the profile of a specific user. It's stored in its user workspace.
     *
     * @param userName the user name
     * @param session the current CoreSession
     * @return the user profile DocumentModel
     */
    DocumentModel getUserProfileDocument(String userName, CoreSession session);

    /**
     * Get a DocumentModel containing both user and user profile schemas
     *
     * @param userModel the user DocumentModel
     * @param session the current CoreSession
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
