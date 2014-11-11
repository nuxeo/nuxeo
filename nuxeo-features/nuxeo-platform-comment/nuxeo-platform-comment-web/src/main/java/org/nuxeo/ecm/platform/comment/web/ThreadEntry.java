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

package org.nuxeo.ecm.platform.comment.web;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:frederic.baude@gmail.com">Frederic Baude</a>
 *
 */
public class ThreadEntry implements Serializable {

    private static final long serialVersionUID = 8765190624691092L;

    DocumentModel comment;

    int depth;

    public ThreadEntry(DocumentModel comment, int depth) {
        this.comment = comment;
        this.depth = depth;
    }

    public DocumentModel getComment() {
        return comment;
    }

    // TODO: remove for 5.4 unless there is an issue with that
    @Deprecated
    public void setComment(DocumentModel comment) {
        this.comment = comment;
    }

    public int getDepth() {
        return depth;
    }

    // TODO: remove for 5.4 unless there is an issue with that
    @Deprecated
    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getId() {
        return comment.getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ThreadEntry)) {
            return false;
        }
        ThreadEntry other = (ThreadEntry) obj;
        String id = getId();
        String otherId = other.getId();
        return id == null ? otherId == null : id.equals(otherId);
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
