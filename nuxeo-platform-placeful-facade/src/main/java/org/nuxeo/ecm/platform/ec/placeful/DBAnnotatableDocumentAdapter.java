/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: DBAnnotatableDocumentAdapter.java 29556 2008-01-23 00:59:39Z jcarsique $
 */
package org.nuxeo.ecm.platform.ec.placeful;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ec.placeful.ejb.interfaces.EJBPlacefulService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 *
 */
public class DBAnnotatableDocumentAdapter implements DBAnnotatableDocument {

    private final DocumentModel doc;
    private EJBPlacefulService service;

    public DBAnnotatableDocumentAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    public Object getAnnotation(String name) throws ClientException {
        EJBPlacefulService bean = getServiceBean();
        return bean.getAnnotation(doc.getId(), name);
    }

    public void setAnnotation(String name, Object value) throws ClientException {
        EJBPlacefulService bean = getServiceBean();
        bean.setAnnotation((Annotation) value);
    }

    private EJBPlacefulService getServiceBean() throws ClientException {
        if (service == null) {
            try {
                service = Framework.getService(EJBPlacefulService.class);
            } catch (Exception e) {
                throw new ClientException("Failed to get placeful service", e);
            }
        }
        return service;
    }

}
