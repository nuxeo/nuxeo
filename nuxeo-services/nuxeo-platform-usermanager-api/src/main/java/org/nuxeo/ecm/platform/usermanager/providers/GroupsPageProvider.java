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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Default Groups Provider
 *
 * @since 5.4.2
 */
public class GroupsPageProvider extends
        AbstractGroupsPageProvider<DocumentModel> {

    @Override
    public List<DocumentModel> getCurrentPage() {
        return computeCurrentPage();
    }

}
