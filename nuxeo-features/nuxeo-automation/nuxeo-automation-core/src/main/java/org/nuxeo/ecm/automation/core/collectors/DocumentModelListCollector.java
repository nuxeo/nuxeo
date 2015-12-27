/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.collectors;

import java.util.ArrayList;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OutputCollector;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * This implementation collect {@link DocumentModel} objects and return them as a {@link DocumentModelList} object.
 * <p>
 * You may use this to automatically iterate over iterable inputs in operation methods that <b>return</b> a
 * {@link DocumentModel} object
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentModelListCollector extends ArrayList<DocumentModel> implements DocumentModelList,
        OutputCollector<DocumentModelList, DocumentModelList> {

    private static final long serialVersionUID = 5732663048354570870L;

    @Override
    public long totalSize() {
        return size();
    }

    @Override
    public void collect(OperationContext ctx, DocumentModelList obj) throws OperationException {
        if (obj != null) {
            addAll(obj);
        }
    }

    @Override
    public DocumentModelList getOutput() {
        return this;
    }
}
