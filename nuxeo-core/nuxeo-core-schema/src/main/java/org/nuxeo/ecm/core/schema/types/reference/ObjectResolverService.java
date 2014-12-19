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
 * Provides a way to instanciate {@link ObjectResolver}.
 *
 * @since 7.1
 */
public interface ObjectResolverService {

    /**
     * @param type the xsd type, a resolver could manage.
     * @param parameters the parameters for this resolver.
     * @return
     * @since 7.1
     */
    ObjectResolver getResolver(String type, Map<String, String> parameters);

}
