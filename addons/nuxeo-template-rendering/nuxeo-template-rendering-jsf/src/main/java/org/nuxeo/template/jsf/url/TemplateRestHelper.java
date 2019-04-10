/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.jsf.url;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.url.AbstractRenditionRestHelper;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

@Name("templateRestHelper")
@Scope(ScopeType.PAGE)
public class TemplateRestHelper extends AbstractRenditionRestHelper {

    private static final long serialVersionUID = 1L;

    protected Blob renderAsBlob(DocumentModel doc, String renditionName) {

        TemplateBasedDocument template = doc.getAdapter(TemplateBasedDocument.class);
        if (template != null) {
            return template.renderWithTemplate(renditionName);
        }
        return null;
    }
}
