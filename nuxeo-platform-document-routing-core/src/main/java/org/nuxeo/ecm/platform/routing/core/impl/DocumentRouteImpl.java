/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;

/**
 * @author arussel
 *
 */
public class DocumentRouteImpl implements DocumentRoute {
    private DocumentModel doc;

    public DocumentRouteImpl(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public boolean isInstance() {
        return false;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isEnded() {
        return false;
    }

    @Override
    public void start(CoreSession session) {
    }

    @Override
    public List<DocumentRouteStep> getCurrentSteps() {
        return null;
    }

    @Override
    public List<DocumentRouteStep> getAllSteps() {
        return null;
    }

    @Override
    public List<DocumentRouteStep> getDoneSteps() {
        return null;
    }

    @Override
    public List<DocumentRouteStep> getTodoSteps() {
        return null;
    }

    @Override
    public void setAttachedDocuments(List<String> docIds) {

    }

    @Override
    public DocumentModel getDocument() {
        return doc;
    }

    @Override
    public void save(CoreSession session) {
        try {
            session.saveDocument(doc);
            session.save();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

}
