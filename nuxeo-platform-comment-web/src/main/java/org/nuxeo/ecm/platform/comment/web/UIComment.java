/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class UIComment implements Comparable, Serializable {

    private static final long serialVersionUID = 2457051749449691092L;

    //final Comment comment;

    private final DocumentModel comment;

    private final UIComment parent;

    private List<UIComment> children;

    public UIComment(UIComment parent, DocumentModel docModel) {
        this.parent = parent;
        comment = docModel;
        children = new ArrayList<UIComment>();
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

    public DataModel getDataModel() {
        return new ListDataModel(children);
    }

    //TODO : override equals and hashCode
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

    public int compareTo(Object o) {
        if (!(o instanceof UIComment)) {
            return -1;
        }

        DocumentModel other = ((UIComment) o).comment;
        Calendar myDate;
        try {
            myDate = (Calendar) comment.getProperty("dublincore",
                    "created");
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        Calendar otherDate;
        try {
            otherDate = (Calendar) other.getProperty("dublincore",
                    "created");
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        return myDate.compareTo(otherDate);
    }

    public DocumentModel getComment() {
        return comment;
    }

}
