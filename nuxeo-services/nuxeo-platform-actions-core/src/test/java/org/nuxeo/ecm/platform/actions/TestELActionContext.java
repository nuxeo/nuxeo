/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.jboss.el.ExpressionFactoryImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestELActionContext {

    @Inject
    protected CoreSession session;

    @Test
    public void shouldValidateAndEvaluateExpression() {
        DocumentModel document = session.createDocumentModel("/", "anyFile", "File");
        document = session.createDocument(document);

        ActionContext actionContext = getActionContext(document);

        String expression = "document.getPathAsString().startsWith('/testFile')";
        assertTrue(actionContext.isValid(expression));
        assertFalse(actionContext.checkCondition(expression));

        expression = "document.getPathAsString().startsWith('/anyFile')";
        assertTrue(actionContext.isValid(expression));
        assertTrue(actionContext.checkCondition(expression));

        assertTrue(actionContext.isValid("'Nuxeo'.equals('Nuxeo')"));
        assertTrue(actionContext.isValid("1==1"));

        assertFalse(actionContext.isValid("1255475"));
    }

    protected ActionContext getActionContext(DocumentModel document) {
        ELActionContext context = new ELActionContext(new ExpressionContext(), new ExpressionFactoryImpl());
        context.setCurrentPrincipal(session.getPrincipal());
        context.setCurrentDocument(document);
        return context;
    }

}
