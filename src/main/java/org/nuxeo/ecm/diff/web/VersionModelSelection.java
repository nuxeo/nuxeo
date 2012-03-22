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
import org.nuxeo.ecm.platform.query.api.PageSelection;

/**
 * Entry wrapping selection information for a given {@link VersionModel} entry.
 *
 * @author Antoine Taillefer
 */
public class VersionModelSelection extends PageSelection<VersionModel>
        implements VersionModel {

    private static final long serialVersionUID = -6540885573328906786L;

    public VersionModelSelection(VersionModel versionModel, boolean selected) {
        super(versionModel, selected);
    }

    public String getId() {
        return data.getId();
    }

    public void setId(String id) {
        data.setId(id);
    }

    public Calendar getCreated() {
        return data.getCreated();
    }

    public void setCreated(Calendar created) {
        data.setCreated(created);
    }

    public String getDescription() {
        return data.getDescription();
    }

    public void setDescription(String description) {
        data.setDescription(description);
    }

    public String getLabel() {
        return data.getLabel();
    }

    public void setLabel(String label) {
        data.setLabel(label);
    }
}
