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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.computedgroups.UserManagerWithComputedGroups;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantUserManager extends UserManagerWithComputedGroups {

    protected static final Integer CACHE_CONCURRENCY_LEVEL = 10;

    protected static final Integer CACHE_MAXIMUM_SIZE = 1000;

    protected static final Integer CACHE_TIMEOUT = 10;

    protected final Cache<String, NuxeoPrincipal> principalCache = CacheBuilder.newBuilder().concurrencyLevel(
            CACHE_CONCURRENCY_LEVEL).maximumSize(CACHE_MAXIMUM_SIZE).expireAfterWrite(
            CACHE_TIMEOUT, TimeUnit.MINUTES).build();

    @Override
    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry,
            boolean anonymous, List<String> groups) throws ClientException {
        NuxeoPrincipal nuxeoPrincipal = super.makePrincipal(userEntry,
                anonymous, groups);
        if (nuxeoPrincipal instanceof NuxeoPrincipalImpl) {
            nuxeoPrincipal = new MultiTenantPrincipal(
                    (NuxeoPrincipalImpl) nuxeoPrincipal);
        }
        return nuxeoPrincipal;
    }

    protected boolean useCache() {
        return !Framework.isTestModeSet();
    }

    @Override
    public NuxeoPrincipal getPrincipal(String username) throws ClientException {
        if (!useCache()) {
            return super.getPrincipal(username);
        }
        NuxeoPrincipal principal = principalCache.getIfPresent(username);
        if (principal == null) {
            principal = super.getPrincipal(username);
            if (principal!=null) {
                principalCache.put(username, principal);
            }
        }
        return principal;
    }

    @Override
    protected void notifyUserChanged(String userName) throws ClientException {
        if (useCache()) {
            principalCache.invalidate(userName);
        }
        super.notifyUserChanged(userName);
    }

    @Override
    protected void notifyGroupChanged(String groupName) throws ClientException {
        if (useCache()) {
            principalCache.invalidateAll();
        }
        super.notifyGroupChanged(groupName);
    }

}
