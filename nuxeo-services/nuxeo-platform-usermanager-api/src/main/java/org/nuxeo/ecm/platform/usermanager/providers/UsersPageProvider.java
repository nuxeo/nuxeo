/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * This page provider requires two parameters: the first one to be filled with the search string, and the second one to
 * be filled with the selected letter when using the {@code tabbed} listing mode.
 * <p>
 * This page provider requires the property {@link #USERS_LISTING_MODE_PROPERTY} to be filled with a the listing mode to
 * use.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public class UsersPageProvider extends AbstractUsersPageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    @Override
    public List<DocumentModel> getCurrentPage() {
        return computeCurrentPage();
    }

}
