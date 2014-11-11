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
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

@XObject(value = "documentsList")
public class DocumentsListDescriptor implements Serializable {

    private static final long serialVersionUID = 187652786580987097L;

    @XNode("@name")
    private String name;

    @XNode("category")
    private String category = "";

    @XNode("defaultInCategory")
    private boolean defaultInCategory;

    @XNodeList(value = "events/event", type = ArrayList.class, componentType = String.class)
    private List<String> eventsName;

    private String imageURL;

    @XNode("supportAppends")
    boolean supportAppends = true;

    @XNode("readOnly")
    boolean readOnly;

    @XNode("isSession")
    boolean isSession = true;

    @XNode("title")
    String title = "";

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("persistent")
    boolean persistent;

    // empty constructor needed for descriptor instantiation
    public DocumentsListDescriptor() {
        eventsName = new ArrayList<String>();
        imageURL = VirtualHostHelper.getContextPathProperty() + "/icons/clipboard.gif";
    }

    public DocumentsListDescriptor(String listName) {
        this();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean getDefaultInCategory() {
        return defaultInCategory;
    }

    public void setDefaultInCategory(boolean defaultInCategory) {
        this.defaultInCategory = defaultInCategory;
    }

    public List<String> getEventsName() {
        return eventsName;
    }

    public void setEvenstName(List<String> eventsName) {
        this.eventsName = eventsName;
    }

    public boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean getSupportAppends() {
        return supportAppends;
    }

    public void setSupportAppends(boolean supportAppends) {
        this.supportAppends = supportAppends;
    }

    public String getImageURL() {
        return imageURL;
    }

    @XNode("imageURL")
    public void setImageURL(String imageURL) {
        this.imageURL = Framework.expandVars(imageURL);
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

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getIsSession() {
        return isSession;
    }

    public void setIsSession(boolean isSession) {
        this.isSession = isSession;
    }

    public boolean getPersistent() {
        if (!isSession) {
            return false; // XXX conversation scoped list can't be persistent
        }
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

}
