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

package org.nuxeo.ecm.webengine.atom;

import javax.servlet.http.HttpServletRequest;

import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.servlet.ServletRequestContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AbderaHelper {

    public final static HttpServletRequest getHttpServletRequest(RequestContext request) {
      return ((ServletRequestContext)request).getRequest();
    }

    public final static UserSession getUserSession(RequestContext request) {
        return UserSession.getCurrentSession(((ServletRequestContext)request).getRequest().getSession(true));
    }

    public final static CoreSession getCoreSession(RequestContext request) {
        return getUserSession(request).getCoreSession();
    }

    //TODO: temporary method
    public  static CoreSession openSession() throws Exception {
        return openSession("default");
    }
    public  static CoreSession openSession(String repoName) throws Exception {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        Repository repo = rm.getRepository(repoName);
        if (repo == null) {
            throw new ClientException("Unable to get " + repoName
                    + " repository");
        }
        return repo.open();
    }

}
