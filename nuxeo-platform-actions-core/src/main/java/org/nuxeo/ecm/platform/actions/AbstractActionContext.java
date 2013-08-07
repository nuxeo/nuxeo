/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    protected Map<String, Object> localVariables = new HashMap<String, Object>();

    public abstract boolean checkCondition(String expression)
            throws ELException;

    public final void setCurrentDocument(DocumentModel doc) {
        currentDocument = doc;
    }

    public final DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    public final CoreSession getDocumentManager() {
        return docMgr;
    }

    public final void setDocumentManager(CoreSession docMgr) {
        this.docMgr = docMgr;
    }

    public final NuxeoPrincipal getCurrentPrincipal() {
        return currentPrincipal;
    }

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

    public int size() {
        return localVariables.size();
    }

    @Override
    public boolean disableGlobalCaching() {
        return false;
    }

}
