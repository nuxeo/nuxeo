/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.actions;

import java.util.HashMap;
import java.util.Map;

import javax.el.ELException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @since 5.7.3
 */
public abstract class AbstractActionContext implements ActionContext {

    private static final long serialVersionUID = 1L;

    protected DocumentModel currentDocument;

    protected CoreSession docMgr;

    protected NuxeoPrincipal currentPrincipal;

    protected Map<String, Object> localVariables = new HashMap<>();

    @Override
    public abstract boolean checkCondition(String expression) throws ELException;

    @Override
    public final void setCurrentDocument(DocumentModel doc) {
        currentDocument = doc;
    }

    @Override
    public final DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    @Override
    public final CoreSession getDocumentManager() {
        return docMgr;
    }

    @Override
    public final void setDocumentManager(CoreSession docMgr) {
        this.docMgr = docMgr;
    }

    @Override
    public final NuxeoPrincipal getCurrentPrincipal() {
        return currentPrincipal;
    }

    @Override
    public final void setCurrentPrincipal(NuxeoPrincipal currentPrincipal) {
        this.currentPrincipal = currentPrincipal;
    }

    @Override
    public Object getLocalVariable(String key) {
        return localVariables.get(key);
    }

    @Override
    public Object putLocalVariable(String key, Object value) {
        return localVariables.put(key, value);
    }

    @Override
    public void putAllLocalVariables(Map<String, Object> vars) {
        localVariables.putAll(vars);
    }

    @Override
    public int size() {
        return localVariables.size();
    }

    @Override
    public boolean disableGlobalCaching() {
        return false;
    }

}
