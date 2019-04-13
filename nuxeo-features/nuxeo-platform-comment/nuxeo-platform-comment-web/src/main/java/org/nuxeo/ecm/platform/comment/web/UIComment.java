/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class UIComment implements Comparable<Object>, Serializable {

    private static final long serialVersionUID = 2457051749449691092L;

    // final Comment comment;

    private final DocumentModel comment;

    private final UIComment parent;

    private List<UIComment> children;

    public UIComment(UIComment parent, DocumentModel docModel) {
        this.parent = parent;
        comment = docModel;
        children = new ArrayList<>();
    }

    public String getId() {
        return comment.getId();
    }

    public List<UIComment> getChildren() {
        return children;
    }

    public void setChildren(List<UIComment> children) {
        this.children = children;
    }

    public boolean addChild(UIComment child) {
        return children.add(child);
    }

    public UIComment getParent() {
        return parent;
    }

    public boolean removeChild(UIComment child) {
        return children.remove(child);
    }

    public DataModel<UIComment> getDataModel() {
        return new ListDataModel<>(children);
    }

    // TODO : override equals and hashCode
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UIComment)) {
            return false;
        }
        UIComment temp = (UIComment) other;
        return comment.getId().equals(temp.comment.getId());
    }

    @Override
    public int hashCode() {
        return comment.getId().hashCode();
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof UIComment)) {
            return -1;
        }
        DocumentModel other = ((UIComment) o).comment;
        Calendar myDate = (Calendar) comment.getProperty("dublincore", "created");
        Calendar otherDate = (Calendar) other.getProperty("dublincore", "created");
        return myDate.compareTo(otherDate);
    }

    public DocumentModel getComment() {
        return comment;
    }

}
