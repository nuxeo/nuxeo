/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 * *
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.platform.usermanager.GroupConfig;
import org.nuxeo.runtime.api.Framework;

/**
 * Computed group implementation class. Delegates part of the implementation logic to the {@link ComputedGroupsService}
 * that is pluggable.
 *
 * @author Thierry Delprat
 */
public class NuxeoComputedGroup implements ComputedGroup {

    private static final long serialVersionUID = 1L;

    protected List<String> members;

    protected List<String> subGroups;

    protected List<String> parents;

    protected DocumentModel model;

    protected GroupConfig config = GroupConfig.DEFAULT;

    public NuxeoComputedGroup(String name, GroupConfig config) {
        this(name, null, config);
    }

    public NuxeoComputedGroup(String name, String label, GroupConfig config) {
        this.config = config;
        model = SimpleDocumentModel.empty();
        model.setProperty(config.schemaName, config.idField, name);
        model.setProperty(config.schemaName, config.labelField, label);
    }

    /**
     * @deprecated since 9.3. Use {@link #NuxeoComputedGroup(String, GroupConfig)}.
     */
    @Deprecated
    public NuxeoComputedGroup(String name) {
        this(name, GroupConfig.DEFAULT);
    }

    /**
     * @deprecated since 9.3. Use {@link #NuxeoComputedGroup(String, String, GroupConfig)}.
     */
    @Deprecated
    public NuxeoComputedGroup(String name, String label) {
        this(name, label, GroupConfig.DEFAULT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getMemberUsers() {
        if (members == null) {
            ComputedGroupsService cgs = Framework.getService(ComputedGroupsService.class);
            if (cgs != null) {
                members = cgs.getComputedGroupMembers(getName());
            }
            if (members == null) {
                members = new ArrayList<>();
            }
            model.setProperty(config.schemaName, config.membersField, members);
        }
        return members;
    }

    @Override
    public String getName() {
        return (String) model.getProperty(config.schemaName, config.idField);
    }

    @Override
    public String getLabel() {
        String label = (String) model.getProperty(config.schemaName, config.labelField);
        return label == null ? getName() : label;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getParentGroups() {
        if (parents == null) {
            ComputedGroupsService cgs = Framework.getService(ComputedGroupsService.class);
            if (cgs != null) {
                parents = cgs.getComputedGroupParent(getName());
            }
            if (parents == null) {
                parents = new ArrayList<>();
            }
            model.setProperty(config.schemaName, config.parentGroupsField, parents);
        }
        return parents;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getMemberGroups() {
        if (subGroups == null) {
            ComputedGroupsService cgs = Framework.getService(ComputedGroupsService.class);
            if (cgs != null) {
                subGroups = cgs.getComputedGroupSubGroups(getName());
            }
            if (subGroups == null) {
                subGroups = new ArrayList<>();
            }
            model.setProperty(config.schemaName, config.subGroupsField, subGroups);
        }
        return subGroups;
    }

    @Override
    public void setMemberGroups(List<String> groups) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public void setMemberUsers(List<String> users) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public void setLabel(String label) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public void setParentGroups(List<String> groups) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public DocumentModel getModel() {
        return model;
    }
}
