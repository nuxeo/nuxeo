/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ws;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.api.ws.DocumentLoader;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;


/**
 * @author matic
 *
 */
public class DocumentStateLoader implements DocumentLoader {

    protected static final Log log = LogFactory.getLog(DocumentStateLoader.class);
    
    @Override
    public void fillProperties(DocumentModel doc,
            List<DocumentProperty> props, WSRemotingSession rs) throws ClientException {
        CoreSession repo = rs.getDocumentManager();
        DocumentRef ref = doc.getRef();
        DocumentModel.DocumentModelRefresh dmr = repo.refreshDocument(ref, DocumentModel.REFRESH_STATE, null);
        for (Field f:dmr.getClass().getDeclaredFields()) {
            final String fn = f.getName();
            try {
                final Object fv = f.get(dmr);
                if (fv != null) {
                    DocumentProperty prop = new DocumentProperty(fn, fv.toString());
                    props.add(prop);
                }
            } catch (Exception e) {
                log.error("Cannot fetch value for " + ref + ":" + fn, e);
            }
        }
    }

}
