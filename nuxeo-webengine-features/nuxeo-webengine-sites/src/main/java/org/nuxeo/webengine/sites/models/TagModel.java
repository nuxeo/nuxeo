/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.webengine.sites.models;

import org.nuxeo.theme.models.AbstractModel;

/**
 * Model related to the list with the details about the <b>Tag</b>-s that have
 * been created under a webpage, in the fragment initialization mechanism.
 *
 * @author rux
 */
public class TagModel extends AbstractModel {

    private String label;

    private Boolean isPrivate;

    private Boolean canModify;

    private String id;

    public TagModel(String label, Boolean isPrivate, Boolean canModify) {
        setLabel(label);
        setIsPrivate(isPrivate);
        setCanModify(canModify);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public void setCanModify(Boolean canModify) {
        this.canModify = canModify;
    }

    public Boolean getCanModify() {
        return canModify;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
