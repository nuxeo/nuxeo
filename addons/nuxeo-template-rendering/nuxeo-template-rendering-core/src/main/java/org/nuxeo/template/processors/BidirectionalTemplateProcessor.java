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

package org.nuxeo.template.processors;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

/**
 * Interface for {@link TemplateProcessor} that allow reverse mapping. i.e. update the {@link DocumentModel} from data
 * inside the template file. (This can only be done for some implementation because it depends on the target File
 * format)
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 */
public interface BidirectionalTemplateProcessor extends TemplateProcessor {

    public DocumentModel updateDocumentFromBlob(TemplateBasedDocument templateDocument, String templateName)
            throws Exception;
}
