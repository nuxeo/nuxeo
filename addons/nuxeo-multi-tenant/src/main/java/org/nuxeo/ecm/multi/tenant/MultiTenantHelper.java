/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_ADMINISTRATORS_GROUP_SUFFIX;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_GROUP_PREFIX;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_MEMBERS_GROUP_SUFFIX;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.multi.tenant.cache.SimpleCache;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantHelper {

    private MultiTenantHelper() {
        // helper class
    }

    public static String computeTenantAdministratorsGroup(String tenantId) {
        return TENANT_GROUP_PREFIX + tenantId
                + TENANT_ADMINISTRATORS_GROUP_SUFFIX;
    }

    public static String computeTenantMembersGroup(String tenantId) {
        return TENANT_GROUP_PREFIX + tenantId + TENANT_MEMBERS_GROUP_SUFFIX;
    }

    /**
     * Returns the current tenantId for the given {@code principal}, or from the
     * principal stored in the login stack.
     * <p>
     * The {@code principal} is used if it is a {@link SystemPrincipal}, then
     * the tenantId is retrieved from the Principal matching the
     * {@link SystemPrincipal#getOriginatingUser()}.
     */
    public static String getCurrentTenantId(Principal principal)
            throws ClientException {
        if (principal instanceof SystemPrincipal) {
            UserManager userManager = Framework.getLocalService(UserManager.class);
            String originatingUser = ((SystemPrincipal) principal).getOriginatingUser();
            if (originatingUser != null) {
                NuxeoPrincipal nuxeoPrincipal = userManager.getPrincipal(originatingUser);
                return nuxeoPrincipal.getTenantId();
            } else {
                return null;
            }

        } else {
            return ClientLoginModule.getCurrentPrincipal().getTenantId();
        }
    }

    /**
     * Returns the tenantId for the given {@code username} if any, {@code null}
     * otherwise.
     */
    public static String getTenantId(String username) throws ClientException {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        NuxeoPrincipal nuxeoPrincipal = userManager.getPrincipal(username);
        return nuxeoPrincipal.getTenantId();
    }

    protected static final SimpleCache<String> pathCache = new SimpleCache<String>(
            100);

    /**
     * Returns the path of the tenant document matching the {@code tenantId}, or
     * {@code null} if there is no document matching.
     */
    public static String getTenantDocumentPath(CoreSession session,
            final String tenantId) throws ClientException {

        String path = pathCache.get(tenantId);
        if (path == null) {
            final List<String> paths = new ArrayList<String>();
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    String query = String.format(
                            "SELECT * FROM Document WHERE tenantconfig:tenantId = '%s'",
                            tenantId);
                    List<DocumentModel> docs = session.query(query);
                    if (!docs.isEmpty()) {
                        paths.add(docs.get(0).getPathAsString());
                    }
                }
            }.runUnrestricted();
            path = paths.isEmpty() ? null : paths.get(0);
            if (path != null) {
                pathCache.put(tenantId, path);
            }
        }
        return path;
    }

}
