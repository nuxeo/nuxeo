/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *     Kevin Leturc
 *
 */

package org.nuxeo.ecm.core.versioning;

import org.apache.commons.lang.StringUtils;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.platform.el.ELService;
import org.nuxeo.runtime.api.Framework;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import java.util.List;

/**
 * @since 9.1
 */
public class StandardVersioningPolicyFilter implements VersioningPolicyFilter {

    protected List<String> types;

    protected List<String> facets;

    protected List<String> schemas;

    protected String condition;

    public StandardVersioningPolicyFilter(List<String> types, List<String> facets, List<String> schemas,
            String condition) {
        this.types = types;
        this.facets = facets;
        this.schemas = schemas;
        this.condition = condition;
    }

    @Override
    public boolean test(DocumentModel previousDocument, DocumentModel currentDocument) {
        if (!types.isEmpty() && !types.contains(currentDocument.getType())) {
            return false;
        }
        DocumentType docType = currentDocument.getDocumentType();
        if (!schemas.isEmpty() && !schemas.stream().anyMatch(s -> docType.hasSchema(s))) {
            return false;
        }
        if (!facets.isEmpty() && !facets.stream().anyMatch(f -> docType.hasFacet(f))) {
            return false;
        }
        if (!StringUtils.isBlank(condition)) {
            String cond = condition.trim();
            // compatibility code, as JEXL could resolve that kind of expression:
            // detect if expression is in brackets #{}, otherwise add it
            if (!cond.startsWith("#{") && !cond.startsWith("${") && !cond.endsWith("}")) {
                cond = "#{" + cond + "}";
            }

            ELContext context = Framework.getService(ELService.class).createELContext();
            ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

            VariableMapper vm = context.getVariableMapper();

            // init default variables
            ValueExpression previousDocExpr = expressionFactory.createValueExpression(previousDocument,
                    DocumentModel.class);
            ValueExpression currentDocExpr = expressionFactory.createValueExpression(currentDocument,
                    DocumentModel.class);
            ValueExpression userExpr = expressionFactory.createValueExpression(ClientLoginModule.getCurrentPrincipal(),
                    NuxeoPrincipal.class);
            vm.setVariable("previousDocument", previousDocExpr);
            vm.setVariable("currentDocument", currentDocExpr);
            vm.setVariable("document", currentDocExpr);
            vm.setVariable("principal", userExpr);
            vm.setVariable("currentUser", userExpr);

            // evaluate expression
            ValueExpression ve = expressionFactory.createValueExpression(context, cond, Boolean.class);
            return Boolean.TRUE.equals(ve.getValue(context));
        }
        return true;
    }
}
