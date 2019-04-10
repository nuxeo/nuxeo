/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
     *
     * @param templateDocument
     * @param templateName
     * @return
     */
    public Blob renderTemplate(TemplateBasedDocument templateDocument, String templateName) throws IOException;

    /**
     * Extract parameters from the Template file.
     *
     * @param blob
     * @return List of parameters for this template
     */
    public List<TemplateInput> getInitialParametersDefinition(Blob blob) throws IOException;
}
