/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.client.abdera;

import org.apache.abdera.model.Element;
import org.nuxeo.ecm.client.ContentManager;


/**
 * @author matic
 *
 */
public class DocumentEntryTransformer implements org.apache.commons.collections.Transformer {

    protected final ContentManager client;
    
    DocumentEntryTransformer(ContentManager client) {
        this.client = client;
    }
    public Object transform(Object input) {
        Element abderaElement  = (Element)input;
        return new DocumentEntryAdapter(client, abderaElement);
    }

}
