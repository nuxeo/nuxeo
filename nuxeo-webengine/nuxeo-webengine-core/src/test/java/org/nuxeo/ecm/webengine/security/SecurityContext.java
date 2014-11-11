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

package org.nuxeo.ecm.webengine.security;

import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.Adaptable;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SecurityContext implements Adaptable {

    private static final Log log = LogFactory.getLog(SecurityContext.class);

    protected final CoreSession session;
    protected final DocumentModel doc;

    public SecurityContext(CoreSession session, DocumentModel doc) {
        this.session = session;
        this.doc = doc;
    }

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
