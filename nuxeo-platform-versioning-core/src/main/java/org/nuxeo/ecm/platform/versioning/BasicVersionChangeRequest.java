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

package org.nuxeo.ecm.platform.versioning;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Abstract VersionChangeRequest class that is basically a simple attributes
 * holder.
 *
 * @author <a href="mailto:dm@nuxeo.ro">Dragos Mihalache</a>
 */
public abstract class BasicVersionChangeRequest implements VersionChangeRequest {

    private final RequestSource source;

    private final DocumentModel doc;

    private String wfStateInitial;

    private String wfStateFinal;

    protected BasicVersionChangeRequest(RequestSource rs, DocumentModel doc) {
        source = rs;
        this.doc = doc;
    }

    public RequestSource getSource() {
        return source;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public String getWfFinalState() {
        return wfStateFinal;
    }

    public String getWfInitialState() {
        return wfStateInitial;
    }

    public void setWfStateFinal(String wfStateFinal) {
        this.wfStateFinal = wfStateFinal;
    }

    public void setWfStateInitial(String wfStateInitial) {
        this.wfStateInitial = wfStateInitial;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(BasicVersionChangeRequest.class.getSimpleName());
        buf.append(" {source=");
        buf.append(source);
        buf.append(", initial state=");
        buf.append(wfStateInitial);
        buf.append(", final state=");
        buf.append(wfStateFinal);
        buf.append('}');
        return buf.toString();
    }

}
