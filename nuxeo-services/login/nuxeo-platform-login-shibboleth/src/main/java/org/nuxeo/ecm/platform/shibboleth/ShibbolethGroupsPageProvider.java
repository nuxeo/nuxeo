/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    private static final long serialVersionUID = 1L;

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
            return new ArrayList<>();
        }
        return ShibbolethGroupHelper.searchGroup(searchString);
    }
}
