/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;

public abstract class AbstractRunner implements ElementRunner {

    protected List<DocumentRouteElement> getChildrenElement(CoreSession session, DocumentRouteElement element) {
        DocumentModelList children = session.getChildren(element.getDocument().getRef());
        List<DocumentRouteElement> elements = new ArrayList<>();
        for (DocumentModel model : children) {
            elements.add(model.getAdapter(DocumentRouteElement.class));
        }
        return elements;
    }

    @Override
    public void run(CoreSession session, DocumentRouteElement element, Map<String, Serializable> map) {
        run(session, element);
    }

    @Override
    public void resume(CoreSession session, DocumentRouteElement element, String nodeId, String taskId,
            Map<String, Object> data, String status) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void undo(CoreSession session, DocumentRouteElement element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel(CoreSession session, DocumentRouteElement element) {
        List<DocumentRouteElement> children = getChildrenElement(session, element);
        for (DocumentRouteElement child : children) {
            child.cancel(session);
        }
        element.setCanceled(session);
    }
}
