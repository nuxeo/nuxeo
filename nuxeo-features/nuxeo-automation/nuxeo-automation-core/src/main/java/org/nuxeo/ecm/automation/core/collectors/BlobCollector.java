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
public class BlobCollector extends BlobList implements OutputCollector<Blob, BlobList> {

    private static final long serialVersionUID = 5167860889224514027L;

    @Override
    public void collect(OperationContext ctx, Blob obj)
            throws OperationException {
        add(obj);
    }

    @Override
    public BlobList getOutput() {
        return this;
    }
}
