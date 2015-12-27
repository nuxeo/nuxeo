/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.platform.ws;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.api.ws.DocumentLoader;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;

/**
 * @author matic
 */
public class DocumentStateLoader implements DocumentLoader {

    protected static final Log log = LogFactory.getLog(DocumentStateLoader.class);

    @Override
    public void fillProperties(DocumentModel doc, List<DocumentProperty> props, WSRemotingSession rs)
            {
        CoreSession repo = rs.getDocumentManager();
        DocumentRef ref = doc.getRef();
        DocumentModel.DocumentModelRefresh dmr = repo.refreshDocument(ref, DocumentModel.REFRESH_STATE, null);
        for (Field f : dmr.getClass().getDeclaredFields()) {
            final String fn = f.getName();
            try {
                final Object fv = f.get(dmr);
                if (fv != null) {
                    DocumentProperty prop = new DocumentProperty(fn, fv.toString());
                    props.add(prop);
                }
            } catch (ReflectiveOperationException e) {
                log.error("Cannot fetch value for " + ref + ":" + fn, e);
            }
        }
    }

}
