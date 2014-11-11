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

package org.nuxeo.ecm.platform.modifier.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.modifier.DocModifierException;

public interface DocModifierService {

    /**
     * If the value of the content property is null, this method returns without
     * any further processing.
     *
     * @param eventId
     * @throws DocModifierException if
     *   <li> any of the document fields specified in the descriptor do not exist
     *   <li> other processing error occurs
     */
    void processDocument(DocumentModel doc, String eventId) throws DocModifierException;

}
