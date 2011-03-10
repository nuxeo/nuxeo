/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
public class DocumentModelListCollector extends ArrayList<DocumentModel> implements
        DocumentModelList, OutputCollector<DocumentModelList, DocumentModelList> {

    private static final long serialVersionUID = 5732663048354570870L;

    @Override
    public long totalSize() {
        return size();
    }

    @Override
    public void collect(OperationContext ctx, DocumentModelList obj)
            throws OperationException {
        addAll(obj);
    }

    @Override
    public DocumentModelList getOutput() {
        return this;
    }
}
