/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
