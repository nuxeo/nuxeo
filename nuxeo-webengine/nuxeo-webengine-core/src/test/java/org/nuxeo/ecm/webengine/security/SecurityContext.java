/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.security;

import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SecurityContext implements Adaptable {

    private static final Log log = LogFactory.getLog(SecurityContext.class);

    protected final CoreSession session;

    protected final DocumentModel doc;

    public SecurityContext(CoreSession session, DocumentModel doc) {
        this.session = session;
        this.doc = doc;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DocumentModel.class) {
            return adapter.cast(doc);
        } else if (adapter == CoreSession.class) {
            try {
                return adapter.cast(session);
            } catch (Exception e) {
                log.error(e, e);
            }
        } else if (adapter == Principal.class) {
            return adapter.cast(session.getPrincipal());
        }
        return null;
    }

}
