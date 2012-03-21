/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.web;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.VersionModel;

/**
 * Entry wrapping selection information for given {@link VersionModel} entry.
 *
 * @author Antoine Taillefer
 */
public class VersionModelSelection implements VersionModel {

    private static final long serialVersionUID = -6540885573328906786L;

    protected boolean selected;

    protected VersionModel versionModel;

    public VersionModelSelection(VersionModel versionModel, boolean selected) {
        this.versionModel = versionModel;
        this.selected = selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public VersionModel getVersionModel() {
        return versionModel;
    }

    public void setVersionModel(VersionModel versionModel) {
        this.versionModel = versionModel;
    }

    public String getId() {
        return versionModel.getId();
    }

    public void setId(String id) {
        versionModel.setId(id);
    }

    public Calendar getCreated() {
        return versionModel.getCreated();
    }

    public void setCreated(Calendar created) {
        versionModel.setCreated(created);
    }

    public String getDescription() {
        return versionModel.getDescription();
    }

    public void setDescription(String description) {
        versionModel.setDescription(description);
    }

    public String getLabel() {
        return versionModel.getLabel();
    }

    public void setLabel(String label) {
        versionModel.setLabel(label);
    }
}
