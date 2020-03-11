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

package org.nuxeo.ecm.webengine.security.guards;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.runtime.model.Adaptable;

/**
 * Check access against a built-in permission
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("permission")
public class PermissionGuard implements Guard {

    @XContent
    protected String perm;

    protected PermissionGuard() {
    }

    public PermissionGuard(String perm) {
        this.perm = perm;
    }

    @Override
    public boolean check(Adaptable context) {
        CoreSession session = context.getAdapter(CoreSession.class);
        DocumentModel doc = context.getAdapter(DocumentModel.class);
        return session.hasPermission(doc.getRef(), perm);
    }

    @Override
    public String toString() {
        return "PERM[" + perm + "]";
    }

}
