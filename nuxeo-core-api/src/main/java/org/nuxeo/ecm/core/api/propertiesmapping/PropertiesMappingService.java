/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.api.propertiesmapping;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service that allows to copy a set of metadata from a source to a target
 * document
 *
 * @since 5.6
 */
public interface PropertiesMappingService {

    /**
     * Gets a map of xpaths defining properties on the target and source
     * documents
     *
     * @param mappingName
     * @return
     */
    Map<String, String> getMapping(String mappingName);

    /**
     * Copies the properties defined by the given xpaths in the mapping from the
     * target to the source document. Assumes that the xpaths are valid.
     *
     * @param session
     * @param sourceDoc
     * @param targetDoc
     * @param mappingName
     * @throws ClientException if trying to map incompatible types
     */
    void mapProperties(CoreSession session, DocumentModel sourceDoc,
            DocumentModel targetDoc, String mappingName) throws ClientException;

}