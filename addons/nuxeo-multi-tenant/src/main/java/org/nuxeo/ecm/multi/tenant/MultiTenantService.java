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

import java.security.Principal;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public interface MultiTenantService {

    boolean isTenantIsolationEnabledByDefault();

    String getTenantDocumentType();

    boolean isTenantIsolationEnabled(CoreSession session);

    void enableTenantIsolation(CoreSession session);

    void disableTenantIsolation(CoreSession session);

    void enableTenantIsolationFor(CoreSession session, DocumentModel doc);

    void disableTenantIsolationFor(CoreSession session, DocumentModel doc);

    List<DocumentModel> getTenants();

    boolean isTenantAdministrator(Principal principal);

    List<String> getProhibitedGroups();

}
