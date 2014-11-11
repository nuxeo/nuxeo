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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OutputCollector;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;

/**
 * This implementation collect {@link Blob} objects and return them as a
 * {@link BlobList} object.
 * <p>
 * You may use this to automatically iterate over iterable inputs in operation
 * methods that <b>return</b> a {@link Blob} object.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BlobListCollector extends BlobList implements OutputCollector<BlobList, BlobList> {

    private static final long serialVersionUID = 5167860889224514027L;

    @Override
    public void collect(OperationContext ctx, BlobList obj)
            throws OperationException {
        addAll(obj);
    }

    @Override
    public BlobList getOutput() {
        return this;
    }
}
