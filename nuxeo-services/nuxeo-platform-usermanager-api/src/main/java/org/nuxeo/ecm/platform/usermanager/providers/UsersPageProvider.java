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
 * Page provider listing users.
 * <p>
 * This page provider requires two parameters: the first one to be filled with
 * the search string, and the second one to be filled with the selected letter
 * when using the {@code tabbed} listing mode.
 * <p>
 * This page provider requires the property {@link #USERS_LISTING_MODE_PROPERTY}
 * to be filled with a the listing mode to use.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public class UsersPageProvider extends AbstractUsersPageProvider<DocumentModel> {

    @Override
    public List<DocumentModel> getCurrentPage() {
        return computeCurrentPage();
    }

}
