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

package org.nuxeo.ecm.platform.ui.web.pathelements;

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ArchivedVersionsPathElement implements PathElement {

    public static final String TYPE = "ArchivedVersionsPathElement";

    private static final long serialVersionUID = 8965065837815754773L;

    private final DocumentModel docModel;

    public ArchivedVersionsPathElement(DocumentModel docModel) {
        this.docModel = docModel;
    }

    @Override
    public String getName() {
        FacesContext context = FacesContext.getCurrentInstance();
        return ComponentUtils.translate(context, "label.archivedVersions");
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isLink() {
        return false;
    }

    public DocumentModel getDocumentModel() {
        return docModel;
    }

}
