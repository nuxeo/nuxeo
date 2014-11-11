/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.security;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * A service that provided the UI visible permissions for different document
 * types.
 *
 * @author <a href='mailto:glefter@nuxeo.com'>George Lefter</a>
 * @deprecated use the PermissionProvider that is part of the core
 *             SecurityService instead
 */
@Deprecated
public class UIPermissionService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.webapp.security.UIPermissionService";

    private static final Log log = LogFactory.getLog(UIPermissionService.class);

    private final Map<String, String[]> permissionMap = new HashMap<String, String[]>();

    private String[] defaultPermissionList = new String[0];

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("uiPermissions")) {
            UIPermissionListDescriptor desc = (UIPermissionListDescriptor) contribution;
            if (desc.isDefault) {
                defaultPermissionList = desc.permissions;
            } else {
                permissionMap.put(desc.documentType, desc.permissions);
            }
        } else {
            log.error("unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("uiPermissions")) {
            UIPermissionListDescriptor desc = (UIPermissionListDescriptor) contribution;
            permissionMap.remove(desc.documentType);
        } else {
            log.error("unknown extension point: " + extensionPoint);
        }
    }

    /**
     * Retrieves the visible permissions for a document type.
     *
     * @param documentType the type of document for which to retrieve
     *            permissions, or null to retrieve the default permissions
     * @return the list of permissions for the specified document type
     */
    public String[] getUIPermissions(String documentType) {
        String[] permissions = permissionMap.get(documentType);
        if (documentType == null || permissions == null) {
            return defaultPermissionList;
        } else {
            return permissions;
        }
    }

}
