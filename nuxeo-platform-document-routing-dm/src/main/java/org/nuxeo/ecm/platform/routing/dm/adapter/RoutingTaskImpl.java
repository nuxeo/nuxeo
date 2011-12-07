/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.dm.adapter;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.task.TaskImpl;

/**
 *
 *
 */
public class RoutingTaskImpl extends TaskImpl implements RoutingTask {

    public RoutingTaskImpl(DocumentModel doc) {
        super(doc);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public DocumentModelList getAttachedDocuments(CoreSession arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DocumentRouteStep getDocumentRouteStep(CoreSession arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRefuseOperationChainId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValidateOperationChainId() {
        // TODO Auto-generated method stub
        return null;
    }


}
