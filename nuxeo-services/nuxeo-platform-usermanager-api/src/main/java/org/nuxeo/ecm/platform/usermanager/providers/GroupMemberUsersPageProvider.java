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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.platform.usermanager.providers;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

import java.util.List;

/**
 * @since 8.2
 */
public class GroupMemberUsersPageProvider extends AbstractGroupMemberPageProvider<NuxeoPrincipal> {

    private static final long serialVersionUID = 1L;

    @Override
    protected List<String> getMembers(NuxeoGroup group) {
        return group.getMemberUsers();
    }

    @Override
    protected NuxeoPrincipal getMember(String id) {
        return getUserManager().getPrincipal(id);
    }
}
