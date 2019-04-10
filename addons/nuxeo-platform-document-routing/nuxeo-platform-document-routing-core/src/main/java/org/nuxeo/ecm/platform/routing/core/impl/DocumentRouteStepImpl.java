/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.STEP_DOCUMENT_DESCRIPTION_TYPE;

/**
 * @author arussel
 *
 */
public class DocumentRouteStepImpl extends DocumentRouteElementImpl implements
        DocumentRouteStep {
    public DocumentRouteStepImpl(DocumentModel doc) {
        super(doc);
    }
    
    @Override
    public String getTypeDescription(){
        return STEP_DOCUMENT_DESCRIPTION_TYPE;
    }
}