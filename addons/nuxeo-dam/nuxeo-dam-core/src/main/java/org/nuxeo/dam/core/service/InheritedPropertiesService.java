/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.core.service;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service to register properties which need to be inherited from one {@code
 * DocumentModel} to another one.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public interface InheritedPropertiesService {

    /**
     * Inherits the registered properties from one {@code DocumentModel} to
     * another. It copies the properties only if the two {@code DocumentModel}s
     * contains the concerned schemas / properties.
     *
     * @param from the {@code DocumentModel} from which get the inherited
     *            properties
     * @param to the {@code DocumentModel} where to set the inherited properties
     * @throws ClientException in case of any error setting the properties
     */
    void inheritProperties(DocumentModel from, DocumentModel to)
            throws ClientException;

    /**
     * Returns an immutable {@code Map} containing the registered {@code
     * InheritedPropertiesDescriptor}s
     */
    Map<String, InheritedPropertiesDescriptor> getInheritedPropertiesDescriptors();

}
