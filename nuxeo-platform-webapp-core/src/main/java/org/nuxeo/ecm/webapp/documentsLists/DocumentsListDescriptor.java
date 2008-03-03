/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.documentsLists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "documentsList")
public class DocumentsListDescriptor implements Serializable {

    private static final long serialVersionUID = 187652786580987097L;

    @XNode("@name")
    private String name;

    @XNode("category")
    private String category;

    @XNode("defaultInCategory")
    private Boolean defaultInCategory;

    @XNodeList(value = "events/event", type = ArrayList.class, componentType = String.class)
    private List<String> eventsName;

    @XNode("imageURL")
    private String imageURL;

    @XNode("supportAppends")
    Boolean supportAppends;

    @XNode("readOnly")
    Boolean readOnly;

    @XNode("isSession")
    Boolean isSession;

    @XNode("title")
    String title;

    @XNode("@enabled")
    Boolean enabled;

    @XNode("persistent")
    Boolean persistent;

    public DocumentsListDescriptor() {
        category = "";
        defaultInCategory = false;
        eventsName = new ArrayList<String>();
        supportAppends = true;
        readOnly = false;
        imageURL = "/nuxeo/img/clipboard.gif";
        title = "";
        isSession = true;
        enabled = true;
        persistent=false;
    }

    public DocumentsListDescriptor(String listName) {
        this();
        name = listName;
    }

    // XXX: not used?
    public DocumentsListDescriptor(String listName, String title,
            String category, Boolean isDefault, List<String> events,
            Boolean appendMode, Boolean readOnly, String imgUrl, Boolean isSession) {
        name = listName;
        this.title = title;
        this.category = category;
        defaultInCategory = isDefault;
        eventsName = new ArrayList<String>();
        if (events != null) {
            eventsName.addAll(events);
        }
        supportAppends = appendMode;
        this.readOnly = readOnly;
        imageURL = imgUrl;
        this.isSession = isSession;
        enabled = true;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getDefaultInCategory() {
        return defaultInCategory;
    }

    public void setDefaultInCategory(Boolean defaultInCategory) {
        this.defaultInCategory = defaultInCategory;
    }

    public List<String> getEventsName() {
        return eventsName;
    }

    public void setEvenstName(List<String> eventsName) {
        this.eventsName = eventsName;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Boolean getSupportAppends() {
        return supportAppends;
    }

    public void setSupportAppends(Boolean supportAppends) {
        this.supportAppends = supportAppends;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String listName) {
        name = listName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getIsSession() {
        return isSession;
    }

    public void setIsSession(Boolean isSession) {
        this.isSession = isSession;
    }

    public Boolean getPersistent() {
        if (!isSession) {
            return false; // XXX conversation scoped list can't be persistent
        }
        return persistent;
    }

    public void setPersistent(Boolean persistent) {
        this.persistent = persistent;
    }

}
