/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.comment.web;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:frederic.baude@gmail.com">Frederic Baude</a>
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

    public int getDepth() {
        return depth;
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
