/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * This implementation collect {@link DocumentModel} objects and return them as a
 * {@link DocumentModelList} object.
 * <p>
 * You may use this to automatically iterate over iterable inputs in operation
 * methods that <b>return</b> a {@link DocumentModel} object
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentModelCollector extends ArrayList<DocumentModel> implements
        DocumentModelList, OutputCollector<DocumentModel, DocumentModelList> {

    private static final long serialVersionUID = 5732663048354570870L;

    @Override
    public long totalSize() {
        return size();
    }

    @Override
    public void collect(OperationContext ctx, DocumentModel obj)
            throws OperationException {
        add(obj);
    }

    @Override
    public DocumentModelList getOutput() {
        return this;
    }
}
