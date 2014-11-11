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
package org.nuxeo.ecm.webengine.app.document;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.webengine.app.impl.SuperKeyedRegistry;
import org.nuxeo.runtime.api.Framework;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeRegistry<V> extends SuperKeyedRegistry<DocumentType, V> {

    protected SchemaManager mgr;
    
    public TypeRegistry() throws Exception {
        this (Framework.getService(SchemaManager.class));
    }
    
    public TypeRegistry(SchemaManager mgr) {        
        super (mgr.getDocumentType("Document"));
        this.mgr = mgr;
    }

    @Override
    protected DocumentType getSuperKey(DocumentType key) {
        return (DocumentType)key.getSuperType();
    }

}
