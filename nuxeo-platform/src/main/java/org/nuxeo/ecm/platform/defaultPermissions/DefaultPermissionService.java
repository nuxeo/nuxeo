/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.defaultPermissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class DefaultPermissionService extends DefaultComponent {

    public static final String ID = DefaultPermissionService.class
            .getCanonicalName();

    private static final Log log = LogFactory
            .getLog(DefaultPermissionService.class);

    private final Map<String, ACL> defaultPermissions = new HashMap<String, ACL>();

    @Override
    public void activate(ComponentContext context) throws Exception {
        log.debug("<activate>");
        super.activate(context);
        defaultPermissions.clear();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        log.debug("<deactivate>");
        super.deactivate(context);
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        String extensionPoint = extension.getExtensionPoint();
        if (extensionPoint.equals("config")) {
            registerConfig(extension);
        } else {
            log.warn("unknown extension point: " + extensionPoint);
        }
    }

    public void registerConfig(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            ConfigurationDescriptor config = (ConfigurationDescriptor) contrib;
            for (DocumentDescriptor documentDescriptor : config.getDocuments()) {
                registerDocument(documentDescriptor);
            }
        }
    }

    private void registerDocument(DocumentDescriptor documentDescriptor) {
        String docType = documentDescriptor.getType();
        ACL acl = defaultPermissions.get(docType);
        if (acl == null) {
            acl = new ACLImpl();
            defaultPermissions.put(docType, acl);
        }

        for (PrincipalDescriptor principalDescriptor : documentDescriptor
                .getPrincipals()) {
            String username = principalDescriptor.getName();
            for (PermissionDescriptor permissionDescriptor : principalDescriptor
                    .getPermissions()) {
                String permission = permissionDescriptor.getPermission();
                boolean granted = permissionDescriptor.isGranted();
                ACE ace = new ACE(username, permission, granted);
                acl.add(ace);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        defaultPermissions.clear();
    }

    public ACL getPermissionsForType(String docType) {
        return defaultPermissions.get(docType);
    }

}
