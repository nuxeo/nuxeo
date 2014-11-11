/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.blobholder;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link DocumentModel} adapter factory.
 * Delegates calls to the {@link BlobHolderAdapterService} that management the pluggability for factories.
 *
 * @author tiry
 */
public class BlobHolderAdapterFactory implements DocumentAdapterFactory {

   protected static BlobHolderAdapterService bhas;


   protected BlobHolderAdapterService getService() {
       if (bhas==null) {
           bhas = Framework.getLocalService(BlobHolderAdapterService.class);
       }
       return bhas;
   }

    public Object getAdapter(DocumentModel doc, Class itf) {
        return getService().getBlobHolderAdapter(doc);
    }

}
