/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.usermapper.service;

import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * This service allows to map Nuxeo Users with users coming from external system like SSO or IDM.
 *
 * @author tiry
 *
 */
public interface UserMapperService {

    /**
     * Should retrieve (create if needed) and update the NuxeoPrincipal
     * according to the given userObject
     *
     * @param mappingName the name of the contributed mapping to use
     * @param userObject the native userObject
     * @return the matching {@link NuxeoPrincipal}
     * @throws NuxeoException
     */
    NuxeoPrincipal getCreateOrUpdateNuxeoPrincipal(String mappingName, Object userObject) throws NuxeoException;

    /**
     * Wrap the {@link NuxeoPrincipal} as the userObject used in the external
     * authentication system
     *      *
     * @param mappingName the name of the contributed mapping to use
     * @param principal the {@link NuxeoPrincipal} to wrap
     * @return
     * @throws NuxeoException
     */
    Object wrapNuxeoPrincipal(String mappingName, NuxeoPrincipal principal) throws NuxeoException;

    /**
     * Gives access to the contributed Mapping names
     *
     * @return
     */
    Set<String> getAvailableMappings();
}
