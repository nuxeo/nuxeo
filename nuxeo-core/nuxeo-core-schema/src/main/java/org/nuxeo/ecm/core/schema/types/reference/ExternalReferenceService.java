/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.reference;

import java.util.Map;

/**
 * Provides a way to instanciate {@link ExternalReferenceResolver}.
 *
 * @since 7.1
 */
public interface ExternalReferenceService {

    /**
     * Put this value in the {@link org.nuxeo.ecm.core.api.DocumentModel} to force referenced entity fetching when
     * calling org.nuxeo.ecm.core.api.DocumentModel#setPropertyValue()
     * <p>
     * The associated value is of type {@link Fetching}
     * </p>
     */
    public static final String CTX_MAP_KEY = "ExternalReferenceService.Fetch";

    public static enum Fetching {
        FETCH_ALL, FETCH_NONE;
    }

    /**
     * @param type the xsd type, a resolver could manage.
     * @param parameters the parameters for this resolver.
     * @return
     * @since 7.1
     */
    ExternalReferenceResolver getResolver(String type, Map<String, String> parameters);

}
