/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    mcedica
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.routing.api;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Wraps a documentElement adding informations about the level
 * where the document is inside the container documentRote 
 * 
 * @author <a href="mailto:mcedica@nuxeo.com">Mariana Cedica</a>
 */
public class LocalizableDocumentRouteElement {
    
    DocumentRouteElement element;

    int depth;

    public LocalizableDocumentRouteElement(DocumentRouteElement element, int depth){
        this.element = element;
        this.depth = depth;
    }
    
    public DocumentRouteElement getElement() {
        return element;
    }

    public void setElement(DocumentRouteElement element) {
        this.element = element;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    public DocumentModel getDocument(){
        return element.getDocument();
    }
}