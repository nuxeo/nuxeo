/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.template.api;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

/**
 * Interface used to drive rendition of the {@link TemplateBasedDocument}
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public interface TemplateProcessor {

    /**
     * Perform rendering of the named template against the {@link TemplateBasedDocument}
     */
    Blob renderTemplate(TemplateBasedDocument templateDocument, String templateName) throws IOException;

    /**
     * Extract parameters from the Template file.
     *
     * @return List of parameters for this template
     */
    List<TemplateInput> getInitialParametersDefinition(Blob blob) throws IOException;
}
