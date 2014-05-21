/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.usermanager.providers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * * Page provider listing {@link NuxeoPrincipal}s.
 *
 * @since 5.8
 */
public class NuxeoPrincipalsPageProvider extends
        AbstractUsersPageProvider<NuxeoPrincipal> {

    private static final Log log = LogFactory.getLog(NuxeoPrincipalsPageProvider.class);

    protected List<NuxeoPrincipal> pagePrincipals;

    @Override
    public List<NuxeoPrincipal> getCurrentPage() {
        if (pagePrincipals == null) {
            List<DocumentModel> users = computeCurrentPage();
            pagePrincipals = new ArrayList<>();
            UserManager userManager = Framework.getLocalService(UserManager.class);
            for (DocumentModel user : users) {
                try {
                    NuxeoPrincipal principal = userManager.getPrincipal(user.getProperty(
                            userManager.getUserIdField()).getValue(String.class));
                    pagePrincipals.add(principal);
                } catch (ClientException e) {
                    log.error(e, e);
                }
            }
        }

        return pagePrincipals;
    }

    @Override
    protected void pageChanged() {
        pagePrincipals = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        pagePrincipals = null;
        super.refresh();
    }
}
