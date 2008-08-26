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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest.types;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;

/**
 * Dynamic type generated to handle document types that were not specified
 * explicitly in the configuration
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebDocumentType extends AbstractWebType {

    protected DocumentType docType;
    protected WebType superType;
    protected WebTypeManager mgr;
    protected Class<? extends WebObject> klass;

    public WebDocumentType(WebTypeManager mgr, DocumentType type) {
        this.docType = type;
        this.mgr = mgr;
    }

    public boolean isDynamic() {
        return true;
    }
    public String getName() {
        return docType.getName();
    }

    public WebType getSuperType() {
        if (superType == null) {
            DocumentType stype = (DocumentType)docType.getSuperType();
            if (stype != null) {
                try {
                    superType = mgr.getType(stype.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                    superType = WebType.ROOT;
                }
            } else {
                superType = WebType.ROOT;
            }
        }
        return superType;
    }

    public Class<? extends WebObject> getObjectClass() throws WebException {
        if (klass == null) {
            klass = resolveObjectClass(mgr);
        }
        return klass;
    }



}
