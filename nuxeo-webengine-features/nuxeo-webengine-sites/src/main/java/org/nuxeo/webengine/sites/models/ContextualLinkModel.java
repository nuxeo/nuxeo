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
 * Contributors:
 *     rdarlea
 */
package org.nuxeo.webengine.sites.models;

import org.nuxeo.theme.models.AbstractModel;

/**
 * Model related to the list with the details about the <b>Contextual Link</b>-s
 * that have been created under a <b>WebSite</b> or <b>Webpage</b> document
 * type, in the fragment initialization mechanism.
 *
 * @author rux
 */
public class ContextualLinkModel extends AbstractModel {

    private String title;

    private String description;

    private String link;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ContextualLinkModel(String title, String description, String link) {
        this.title = title;
        this.description = description;
        this.link = link;
    }

}
