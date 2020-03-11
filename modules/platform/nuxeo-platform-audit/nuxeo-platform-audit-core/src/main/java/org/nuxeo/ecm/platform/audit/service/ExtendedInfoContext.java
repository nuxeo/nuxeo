/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.ecm.platform.audit.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

public class ExtendedInfoContext extends ExpressionContext {

    private final EventContext eventContext;

    private final DocumentModel model;

    private final NuxeoPrincipal principal;

    ExtendedInfoContext(EventContext eventContext, DocumentModel model, NuxeoPrincipal principal) {
        this.eventContext = eventContext;
        this.model = model;
        this.principal = principal;
    }

    public void bindVariables(ExpressionEvaluator evaluator) {
        evaluator.bindValue(this, "document", model);
        evaluator.bindValue(this, "message", eventContext);
        evaluator.bindValue(this, "principal", principal);
    }

}
