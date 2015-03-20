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
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_CONFIG_FACET;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_GROUP_PREFIX;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_MEMBERS_GROUP_SUFFIX;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantHelper {

    protected static final Integer CACHE_CONCURRENCY_LEVEL = 10;

    protected static final Integer CACHE_MAXIMUM_SIZE = 1000;

    protected static final Integer CACHE_TIMEOUT = 10;

    protected static final String NO_TENANT = "NO_TENANT";

    protected final static Cache<String, String> pathCache = CacheBuilder.newBuilder().concurrencyLevel(
            CACHE_CONCURRENCY_LEVEL).maximumSize(CACHE_MAXIMUM_SIZE).expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MINUTES).build();

    protected final static Cache<String, String> tenantBinding = CacheBuilder.newBuilder().concurrencyLevel(
            CACHE_CONCURRENCY_LEVEL).maximumSize(CACHE_MAXIMUM_SIZE).expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MINUTES).build();

    private MultiTenantHelper() {
        // helper class
    }

    public static String computeTenantAdministratorsGroup(String tenantId) {
        return TENANT_GROUP_PREFIX + tenantId + TENANT_ADMINISTRATORS_GROUP_SUFFIX;
    }

    public static String computeTenantMembersGroup(String tenantId) {
        return TENANT_GROUP_PREFIX + tenantId + TENANT_MEMBERS_GROUP_SUFFIX;
    }

    /**
     * Returns the current tenantId for the given {@code principal}, or from the principal stored in the login stack.
     * <p>
     * The {@code principal} is used if it is a {@link SystemPrincipal}, then the tenantId is retrieved from the
     * Principal matching the {@link SystemPrincipal#getOriginatingUser()}.
     */
    public static String getCurrentTenantId(Principal principal) throws ClientException {
        if (principal instanceof SystemPrincipal) {
            String originatingUser = ((SystemPrincipal) principal).getOriginatingUser();
            if (originatingUser != null) {
                return getTenantId(originatingUser);
            } else {
                return null;
            }

        } else {
            return ClientLoginModule.getCurrentPrincipal().getTenantId();
        }
    }

    /**
     * Returns the tenantId for the given {@code username} if any, {@code null} otherwise.
     */
    public static String getTenantId(String username) throws ClientException {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        String tenantId = null;
        DocumentModel userModel = userManager.getUserModel(username);
        if (userModel != null) {
            tenantId = (String) userModel.getPropertyValue("user:tenantId");
        }
        return tenantId;
    }

    /**
     * Returns the path of the tenant document matching the {@code tenantId}, or {@code null} if there is no document
     * matching.
     */
    public static String getTenantDocumentPath(CoreSession session, final String tenantId) throws ClientException {
        final List<String> paths = new ArrayList<String>();
        String path = pathCache.getIfPresent(tenantId);
        if (path == null) {
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    String query = String.format("SELECT * FROM Document WHERE tenantconfig:tenantId = '%s'", tenantId);
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

    /**
     * Return the Tenant containing the provided DocumentModel if any
     * 
     * @param doc
     * @return DocumentModel corresponding to the Tenant container, null otherwise
     * @throws ClientException
     */
    public static String getOwningTenantId(final DocumentModel doc) throws ClientException {
        String tenantId = tenantBinding.getIfPresent(doc.getId());
        if (NO_TENANT.equals(tenantId)) {
            return null;
        }

        if (tenantId == null) {
            TenantIdFinder finder = new TenantIdFinder(doc);
            finder.runUnrestricted();
            tenantId = finder.getTenantId();

            if (tenantId == null) {
                tenantBinding.put(doc.getId(), NO_TENANT);
            } else {
                tenantBinding.put(doc.getId(), tenantId);
            }
        }
        return tenantId;
    }

    protected static class TenantIdFinder extends UnrestrictedSessionRunner {

        protected String tenantId;

        protected final DocumentModel target;

        protected TenantIdFinder(DocumentModel target) {
            super(target.getCoreSession());
            this.target = target;
        }

        @Override
        public void run() throws ClientException {
            List<DocumentModel> parents = session.getParentDocuments(target.getRef());
            for (int i = parents.size() - 1; i >= 0; i--) {
                DocumentModel parent = parents.get(i);
                if (parent.hasFacet(TENANT_CONFIG_FACET)) {
                    tenantId = (String) parent.getPropertyValue(Constants.TENANT_ID_PROPERTY);
                    return;
                }
            }
        }

        public String getTenantId() {
            return tenantId;
        }

    }
}
