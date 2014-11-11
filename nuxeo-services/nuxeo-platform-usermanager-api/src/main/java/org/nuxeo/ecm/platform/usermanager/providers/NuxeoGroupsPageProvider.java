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
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Page provider listing {@link NuxeoGroup}s.
 *
 * @since 5.8
 */
public class NuxeoGroupsPageProvider extends
        AbstractGroupsPageProvider<NuxeoGroup> {

    private static final Log log = LogFactory.getLog(NuxeoGroupsPageProvider.class);

    @Override
    public List<NuxeoGroup> getCurrentPage() {
        List<DocumentModel> groups = computeCurrentPage();
        List<NuxeoGroup> nuxeoGroups = new ArrayList<>(groups.size());
        UserManager userManager = Framework.getLocalService(UserManager.class);
        for (DocumentModel group : groups) {
            try {
                NuxeoGroup nuxeoGroup = userManager.getGroup(group.getProperty(
                        userManager.getGroupIdField()).getValue(String.class));
                nuxeoGroups.add(nuxeoGroup);
            } catch (ClientException e) {
                log.error(e, e);
            }
        }
        return nuxeoGroups;
    }
}
