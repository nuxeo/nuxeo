/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.usermanager;

import java.util.List;
import java.util.Objects;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class NuxeoGroupImpl implements NuxeoGroup {

    private static final long serialVersionUID = -69828664399387083L;

    protected DocumentModel model;

    protected GroupConfig config = GroupConfig.DEFAULT;

    public NuxeoGroupImpl(String name) {
        this(name, name);
    }

    public NuxeoGroupImpl(String name, String label) {
        if (name == null) {
            throw new IllegalArgumentException("group name cannot be null");
        }
        name = name.trim();
        label = label == null ? null : label.trim();

        model = SimpleDocumentModel.empty();
        model.setProperty(config.schemaName, config.idField, name);
        model.setProperty(config.schemaName, config.labelField, label);
    }

    public NuxeoGroupImpl(DocumentModel model, GroupConfig config) {
        this.model = model;
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getMemberUsers() {
        return (List<String>) model.getProperty(config.schemaName, config.membersField);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getMemberGroups() {
        return (List<String>) model.getProperty(config.schemaName, config.subGroupsField);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getParentGroups() {
        return (List<String>) model.getProperty(config.schemaName, config.parentGroupsField);
    }

    @Override
    public void setMemberUsers(List<String> users) {
        if (users == null) {
            throw new IllegalArgumentException("member users list cannot be null");
        }
        model.setProperty(config.schemaName, config.membersField, users);
    }

    @Override
    public void setMemberGroups(List<String> groups) {
        if (groups == null) {
            throw new IllegalArgumentException("member groups list cannot be null");
        }
        model.setProperty(config.schemaName, config.subGroupsField, groups);
    }

    @Override
    public void setParentGroups(List<String> groups) {
        if (groups == null) {
            throw new IllegalArgumentException("parent groups list cannot be null");
        }
        model.setProperty(config.schemaName, config.parentGroupsField, groups);
    }

    @Override
    public String getName() {
        return (String) model.getProperty(config.schemaName, config.idField);
    }

    @Override
    public void setName(String name) {
        model.setProperty(config.schemaName, config.idField, name);
    }

    @Override
    public String getLabel() {
        String label = (String) model.getProperty(config.schemaName, config.labelField);
        return label == null ? getName() : label;
    }

    @Override
    public void setLabel(String label) {
        model.setProperty(config.schemaName, config.labelField, label);
    }

    @Override
    public DocumentModel getModel() {
        return model;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof NuxeoGroupImpl) {
            String name = getName();
            String otherName = ((NuxeoGroupImpl) other).getName();
            return Objects.equals(name, otherName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        String name = getName();
        return name == null ? 0 : name.hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }

}
