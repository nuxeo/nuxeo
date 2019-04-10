/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.ActionableObject;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
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
        return session.getDocument(new IdRef(id)).getAdapter(DocumentRouteStep.class);
    }

    @Override
    public DocumentModelList getAttachedDocuments(CoreSession session) {
        return new DocumentModelListImpl();
    }
}
