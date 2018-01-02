/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.el.ELConstants.CURRENT_DOCUMENT;
import static org.nuxeo.ecm.platform.el.ELConstants.CURRENT_USER;
import static org.nuxeo.ecm.platform.el.ELConstants.DOCUMENT;
import static org.nuxeo.ecm.platform.el.ELConstants.PREVIOUS_DOCUMENT;
import static org.nuxeo.ecm.platform.el.ELConstants.PRINCIPAL;

import java.util.Collection;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import org.apache.commons.lang3.StringUtils;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.platform.el.ELService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 9.1
 */
public class StandardVersioningPolicyFilter implements VersioningPolicyFilter {

    protected Collection<String> types;

    protected Collection<String> facets;

    protected Collection<String> schemas;

    protected String condition;

    public StandardVersioningPolicyFilter(Collection<String> types, Collection<String> facets,
            Collection<String> schemas, String condition) {
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
        if (!schemas.isEmpty() && schemas.stream().noneMatch(docType::hasSchema)) {
            return false;
        }
        if (!facets.isEmpty() && facets.stream().noneMatch(docType::hasFacet)) {
            return false;
        }
        if (!StringUtils.isBlank(condition)) {

            String cond = evaluateCondition(condition);

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
            vm.setVariable(PREVIOUS_DOCUMENT, previousDocExpr);
            vm.setVariable(CURRENT_DOCUMENT, currentDocExpr);
            vm.setVariable(DOCUMENT, currentDocExpr);
            vm.setVariable(PRINCIPAL, userExpr);
            vm.setVariable(CURRENT_USER, userExpr);

            // evaluate expression
            ValueExpression ve = expressionFactory.createValueExpression(context, cond, Boolean.class);
            return Boolean.TRUE.equals(ve.getValue(context));
        }
        return true;
    }

    /**
     * Evaluate and build a valid condition
     *
     * @param condition the initial condition
     */
    public static String evaluateCondition(String condition) {

        String cond = condition.trim();
        // compatibility code, as JEXL could resolve that kind of expression:
        // detect if expression is in brackets #{}, otherwise add it
        if (!cond.startsWith("#{") && !cond.startsWith("${") && !cond.endsWith("}")) {
            cond = "#{" + cond + "}";
        }

        // Check if there is a null/not-null evaluation on previousDocument, if not
        // Add a not-null evaluation on it to prevent NPE
        String p1 = ".*" + PREVIOUS_DOCUMENT + "\\..+";
        String p2 = ".*" + PREVIOUS_DOCUMENT + "\\s*[!=]=\\s*null.*";
        if (cond.matches(p1) && !cond.matches(p2)) {
            cond = "#{" + PREVIOUS_DOCUMENT + " != null && (" + cond.substring(2, cond.length() - 1) + ")}";
        }
        return cond;
    }
}
