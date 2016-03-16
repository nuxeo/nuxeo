/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Guillaume Renard"
 */

package org.nuxeo.ecm.platform.usermanager.providers;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.2
 */
public class GroupMemberPageProvider extends AbstractPageProvider<NuxeoPrincipal>
        implements PageProvider<NuxeoPrincipal> {

    private static final long serialVersionUID = 1L;

    protected static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    protected List<NuxeoPrincipal> currentPagePrincipal;

    @Override
    public List<NuxeoPrincipal> getCurrentPage() {
        if (currentPagePrincipal == null) {
            currentPagePrincipal = new ArrayList<NuxeoPrincipal>();
            List<String> usernames = ((NuxeoGroup) getParameters()[0]).getMemberUsers();
            UserManager userManager = Framework.getService(UserManager.class);
            int limit = safeLongToInt(getCurrentPageOffset() + getPageSize());
            if (limit > usernames.size()) {
                limit = usernames.size();
            }
            for (String username : usernames.subList(safeLongToInt(getCurrentPageOffset()), limit)) {
                currentPagePrincipal.add(userManager.getPrincipal(username));

            }
            setResultsCount(usernames.size());
        }
        return currentPagePrincipal;
    }

    @Override
    public long getPageLimit() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            return 0;
        }
        return ((NuxeoGroup) getParameters()[0]).getMemberGroups().size() / pageSize;
    }

    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    protected void pageChanged() {
        currentPagePrincipal = null;
        super.pageChanged();
    }
}
