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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.usermanager;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@WebObject(type = "groups")
public class GroupRootObject extends AbstractUMRootObject<NuxeoGroup> {

    public static final String PAGE_PROVIDER_NAME = "nuxeo_groups_listing";

    @Override
    protected NuxeoGroup getArtifact(String id) {
        return um.getGroup(id);
    }

    @Override
    protected String getArtifactType() {
        return "group";
    }

    @Override
    protected void checkPrecondition(NuxeoGroup group) {
        checkCurrentUserCanCreateArtifact(group);
        checkGroupHasAName(group);
        checkGroupDoesNotAlreadyExists(group, um);
    }

    @Override
    protected NuxeoGroup createArtifact(NuxeoGroup group) {
        um.createGroup(group.getModel());
        return um.getGroup(group.getName());
    }

    private void checkGroupDoesNotAlreadyExists(NuxeoGroup group, UserManager um) {
        if (um.getGroup(group.getName()) != null) {
            throw new NuxeoException("Group already exists", SC_CONFLICT);
        }
    }

    private void checkGroupHasAName(NuxeoGroup group) {
        if (group.getName() == null) {
            throw new NuxeoException("Group MUST have a name", SC_BAD_REQUEST);
        }
    }

    @Override
    boolean isAPowerUserEditableArtifact(NuxeoGroup artifact) {
        return isAPowerUserEditableGroup(artifact);

    }

    static boolean isAPowerUserEditableGroup(NuxeoGroup group) {
        UserManager um = Framework.getService(UserManager.class);
        Set<String> allGroups = computeAllGroups(um, group);
        List<String> administratorsGroups = um.getAdministratorsGroups();
        return allGroups.stream().noneMatch(administratorsGroups::contains);
    }

    protected static Set<String> computeAllGroups(UserManager um, NuxeoGroup group) {
        Set<String> allGroups = new HashSet<>();
        Queue<NuxeoGroup> queue = new LinkedList<>();
        queue.add(group);

        while (!queue.isEmpty()) {
            NuxeoGroup nuxeoGroup = queue.poll();
            allGroups.add(nuxeoGroup.getName());
            nuxeoGroup.getParentGroups()
                      .stream()
                      .filter(pg -> !allGroups.contains(pg))
                      .map(um::getGroup)
                      .forEach(queue::add);
        }

        return allGroups;
    }

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        return ppService.getPageProviderDefinition(PAGE_PROVIDER_NAME);
    }

}
