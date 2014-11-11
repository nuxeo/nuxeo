/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.blobholder;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link DocumentModel} adapter factory. Delegates calls to the
 * {@link BlobHolderAdapterService} that management the pluggability for
 * factories.
 *
 * @author tiry
 */
public class BlobHolderAdapterFactory implements DocumentAdapterFactory {

    protected static BlobHolderAdapterService bhas;

    protected BlobHolderAdapterService getService() {
        if (bhas == null) {
            bhas = Framework.getLocalService(BlobHolderAdapterService.class);
        }
        return bhas;
    }

    @Override
    public Object getAdapter(DocumentModel doc, Class itf) {
        return getService().getBlobHolderAdapter(doc);
    }

}
