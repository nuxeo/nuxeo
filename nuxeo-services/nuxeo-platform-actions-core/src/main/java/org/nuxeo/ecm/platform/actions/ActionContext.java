/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *     <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 *     Anahide Tchertchian
 *
 * $Id: ActionContext.java 20218 2007-06-07 19:19:46Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.Map;

import javax.el.ELException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Interface for action context evaluation
 *
 * @since 5.7.3
 */
public interface ActionContext extends Serializable {

    /**
     * Sets the current document to use for filter evaluation
     */
    void setCurrentDocument(DocumentModel doc);

    /**
     * Returns the current document to use for filter evaluation
     */
    DocumentModel getCurrentDocument();

    /**
     * Sets the core session to use for filter evaluation
     */
    void setDocumentManager(CoreSession docMgr);

    /**
     * Returns the core session to use for filter evaluation
     */
    CoreSession getDocumentManager();

    /**
     * Sets the current principal to use for filter evaluation
     */
    void setCurrentPrincipal(NuxeoPrincipal currentPrincipal);

    /**
     * Returns the current principal to use for filter evaluation
     */
    NuxeoPrincipal getCurrentPrincipal();

    /**
     * Sets a local variable, to put in the context so that expressions can reference it.
     */
    Object putLocalVariable(String key, Object value);

    /**
     * Sets local variables, to put in the context so that expressions can reference them.
     */
    void putAllLocalVariables(Map<String, Object> vars);

    /**
     * Returns a local variable put in the context
     */
    Object getLocalVariable(String key);

    /**
     * Returns the number of local variables
     */
    int size();

    /**
     * Returns true if given expression resolves to true in this context.
     * <p>
     * Returns false if expression is blank (null or empty).
     *
     * @throws ELException
     */
    boolean checkCondition(String expression) throws ELException;

    /**
     * Evaluates the given {@code expression} and returns the result cast to the given {@code expectedType}.
     *
     * @return the result of the expression evaluation
     * @since 10.2
     */
    <T> T evalExpression(String expression, Class<T> expectedType) throws ELException;

    /**
     * Returns true if expressions evaluation should not be cached globally
     */
    boolean disableGlobalCaching();

}
