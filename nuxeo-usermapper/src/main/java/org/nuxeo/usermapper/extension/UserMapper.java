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

package org.nuxeo.usermapper.extension;

import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.usermapper.service.UserMapperService;

/**
 * Interface for class providing a named implementation for the
 * {@link UserMapperService}
 * 
 * @author tiry
 * 
 */
public interface UserMapper {

    /**
     * Should retrieve (create if needed) and update the NuxeoPrincipal
     * according to the given userObject
     * 
     * @param userObject
     * @return
     */
    NuxeoPrincipal getCreateOrUpdateNuxeoPrincipal(Object userObject);

    /**
     * Wrap the {@link NuxeoPrincipal} as the userObject used in the external
     * authentication system
     * 
     * @param principal
     * @return
     */
    Object wrapNuxeoPrincipal(NuxeoPrincipal principal);

    /**
     * Init callback to receive the parameters set inside the descriptor
     * 
     * @param params
     * @throws Exception
     */
    void init(Map<String, String> params) throws Exception;

    /**
     * Release callback : called when the plugin is about to be unloaded
     * 
     */
    void release();
}
