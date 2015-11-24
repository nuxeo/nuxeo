/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.shibboleth;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.providers.GroupsPageProvider;

/**
 * Page provider listing Shibboleth groups
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 * @since 5.4.2
 */
public class ShibbolethGroupsPageProvider extends GroupsPageProvider {

    @Override
    protected List<DocumentModel> searchAllGroups() {
        return ShibbolethGroupHelper.getGroups();
    }

    @Override
    protected List<DocumentModel> searchGroups() {
        String searchString = getFirstParameter();
        if ("*".equals(searchString)) {
            return searchAllGroups();
        } else if ("".equals(searchString)) {
            return new ArrayList<DocumentModel>();
        }
        return ShibbolethGroupHelper.searchGroup(searchString);
    }
}
