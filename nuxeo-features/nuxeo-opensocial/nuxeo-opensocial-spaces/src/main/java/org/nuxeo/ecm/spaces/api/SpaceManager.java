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

package org.nuxeo.ecm.spaces.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;

/**
 * Framework service for CRUD operations concerning Univers, Space in a specific
 * univers, and Gadget in a specific space
 *
 * @author 10044893
 *
 */
public interface SpaceManager {

    Space getSpace(String spaceProviderName, CoreSession session,
            DocumentModel contextDocument, String spaceName, Map<String, String> parameters)
            throws SpaceException;

    Space getSpace(String spaceProviderName, CoreSession session,
            DocumentModel contextDocument, String spaceName)
            throws SpaceException;

    Space getSpace(String spaceProviderName, CoreSession session,
            DocumentModel contextDocument) throws SpaceException;

    Space getSpace(String spaceProviderName, CoreSession session)
            throws SpaceException;

    Space getSpace(String spaceProviderName, DocumentModel contextDocument,
            String spaceName) throws SpaceException;

    Space getSpace(String spaceProviderName, DocumentModel contextDocument)
            throws SpaceException;

    Space getSpaceFromId(String spaceId, CoreSession session)
            throws SpaceException;


    Collection<SpaceProvider> getSpaceProviders();

    SpaceProvider getSpaceProvider(String string) throws SpaceException;

    List<String> getAvailablePermissions();
    
}
