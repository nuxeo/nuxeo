/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Stéphane Lacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.business;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This operation map pojo client side to document adapter server side and create NX document assuming that pojo and
 * adapter have both properties in common.
 *
 * @since 5.7
 */
@Operation(id = BusinessCreateOperation.ID, category = Constants.CAT_BUSINESS, label = "BusinessCreateOperation", description = "This operation map pojo client side to document adapter server side and create NX document assuming that pojo and adapter have both properties in common.", addToStudio = false)
public class BusinessCreateOperation {

    public static final String ID = "Business.BusinessCreateOperation";

    @Context
    protected CoreSession session;

    @Param(name = "parentPath", required = true)
    protected String parentPath;

    @Param(name = "name", required = true)
    protected String name;

    @OperationMethod
    public BusinessAdapter run(BusinessAdapter input) {
        DocumentModel document = input.getDocument();
        // TODO the code intends to copy to a new document with parentPath+name but it's buggy and does not do that
        // TODO currently we just create a placeless document with an random name
        // TODO use createDocumentModel instead of document to fix
        // createDocumentModel.copyContent(document);
        document = session.createDocument(document);
        return document.getAdapter(input.getClass());
    }

}
