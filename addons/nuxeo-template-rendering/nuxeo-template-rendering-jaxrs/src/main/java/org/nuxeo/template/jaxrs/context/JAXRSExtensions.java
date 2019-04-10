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
package org.nuxeo.template.jaxrs.context;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.context.DocumentWrapper;

public class JAXRSExtensions {

    protected final DocumentModel doc;

    protected final DocumentWrapper nuxeoWrapper;

    protected final String templateName;

    public JAXRSExtensions(DocumentModel doc, DocumentWrapper nuxeoWrapper, String templateName) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
        this.templateName = templateName;
    }

    protected static String getContextPathProperty() {
        return Framework.getProperty("org.nuxeo.ecm.contextPath", "/nuxeo");
    }

    public String getResourceUrl(String resourceName) {
        StringBuilder sb = new StringBuilder(getContextPathProperty());
        sb.append("/site/templates/doc/");
        sb.append(doc.getId());
        sb.append("/resource/");
        sb.append(templateName);
        sb.append("/");
        sb.append(resourceName);
        return sb.toString();
    }
}
