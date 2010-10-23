/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.webdav.provider;

import com.sun.jersey.api.core.HttpContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.Util;

import javax.ws.rs.Consumes;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;

@Provider
public class CoreSessionProvider extends AbstractInjectableProvider<CoreSession> {

    public CoreSessionProvider() {
        super(CoreSession.class);
    }

    @Override
    public CoreSession getValue(HttpContext c) {
        try {
            Util.startTransaction();
            return Util.getSession();
        } catch (Exception e) {
            return null;
        }
    }

}
