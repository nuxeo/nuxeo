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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component to manage the pluggable factory for {@link DocumentAdapterFactory}
 * Also provides the service interface {@link BlobHolderAdapterService}
 *
 * @author tiry
 *
 */
public class BlobHolderAdapterComponent extends DefaultComponent  implements BlobHolderAdapterService{

    protected static Map<String, BlobHolderFactory> factories = new HashMap<String, BlobHolderFactory>();

    public static final String BLOBHOLDERFACTORY_EP ="BlobHolderFactory";

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {

        if (BLOBHOLDERFACTORY_EP.equals(extensionPoint)) {
            BlobHolderFactoryDescriptor desc = (BlobHolderFactoryDescriptor) contribution;
            factories.put(desc.getDocType(), desc.getFactory());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
    }



    /** for test **/

    public static Set<String> getFactoryNames() {
        return factories.keySet();
    }

    /** Service Interface **/
    public BlobHolder getBlobHolderAdapter(DocumentModel doc) {

        if (factories.containsKey(doc.getType())) {
            BlobHolderFactory factory = factories.get(doc.getType());
            return factory.getBlobHolder(doc);
        }

        if (doc.hasSchema("file")) {
            return new DocumentBlobHolder(doc, "file:content");
        } else if (doc.hasSchema("note")) {
            return new DocumentStringBlobHolder(doc,"note:note");
        }
        return null;
    }

}
