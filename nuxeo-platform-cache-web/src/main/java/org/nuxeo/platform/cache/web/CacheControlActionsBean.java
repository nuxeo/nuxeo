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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.platform.cache.web;

import static org.jboss.seam.ScopeType.SESSION;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.cache.CacheService;
import org.nuxeo.ecm.platform.cache.client.ClientCacheServiceFactory;

/**
 * Defines actions that will be used to control objects cached on the client
 * side of the cache.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Name("cacheControlActions")
@Scope(SESSION)
public class CacheControlActionsBean implements CacheControlActions {

    private static final Log log = LogFactory.getLog(CacheControlActionsBean.class);

    @Destroy
    public void destroy() {
        log.debug("@Destroy");
    }

    public void clearClientCache() {
        final CacheService cacheService = ClientCacheServiceFactory.getCacheService();

        /*
        try {
            cacheService.list();
        } catch (CacheServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
    }
}
