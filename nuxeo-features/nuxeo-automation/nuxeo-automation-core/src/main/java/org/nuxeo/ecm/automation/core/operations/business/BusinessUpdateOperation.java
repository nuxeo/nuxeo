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
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.business;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This operation map pojo client side to document adapter server side and update the related NX document.
 *
 * @since 5.7
 */
@Operation(id = BusinessUpdateOperation.ID, category = Constants.CAT_BUSINESS, label = "BusinessUpdateOperation", description = "This operation map pojo client side to document adapter server side and update the related NX document.", addToStudio = false)
public class BusinessUpdateOperation {

    public static final String ID = "Business.BusinessUpdateOperation";

    @Context
    protected CoreSession session;

    @OperationMethod
    public BusinessAdapter run(BusinessAdapter input) throws ClassNotFoundException {
        DocumentModel document = input.getDocument();
        session.saveDocument(document);
        return input;
    }

}
