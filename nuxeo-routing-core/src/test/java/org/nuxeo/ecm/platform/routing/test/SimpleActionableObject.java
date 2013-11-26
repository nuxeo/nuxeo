/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.ActionableObject;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class SimpleActionableObject implements ActionableObject {
    protected String id;

    public SimpleActionableObject(String id) {
        this.id = id;
    }

    @Override
    public String getValidateOperationChainId() {
        return "simpleValidate";
    }

    @Override
    public String getRefuseOperationChainId() {
        return "simpleRefuse";
    }

    @Override
    public DocumentRouteStep getDocumentRouteStep(CoreSession session) {
        try {
            return session.getDocument(new IdRef(id)).getAdapter(
                    DocumentRouteStep.class);
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DocumentModelList getAttachedDocuments(CoreSession session) {
        return new DocumentModelListImpl();
    }
}
