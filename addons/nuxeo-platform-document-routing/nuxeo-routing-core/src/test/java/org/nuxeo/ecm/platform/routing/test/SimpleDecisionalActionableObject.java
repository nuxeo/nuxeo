/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.ActionableObject;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;

/**
 * <!-- @deprecated since 5.9.2 - Use only routes of type 'graph' -->
 */
@Deprecated
public class SimpleDecisionalActionableObject implements ActionableObject {

    String stepDocId;

    public SimpleDecisionalActionableObject(String stepDocId) {
        this.stepDocId = stepDocId;
    }

    @Override
    public DocumentModelList getAttachedDocuments(CoreSession session) {
        return new DocumentModelListImpl();
    }

    @Override
    public DocumentRouteStep getDocumentRouteStep(CoreSession session) {
        DocumentModel docStep = session.getDocument(new IdRef(stepDocId));
        return docStep.getAdapter(DocumentRouteStep.class);
    }

    @Override
    public String getRefuseOperationChainId() {
        return "simpleRefuse";
    }

    @Override
    public String getValidateOperationChainId() {
        return "decideNextStepAndSimpleValidate";
    }

}
