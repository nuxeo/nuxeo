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
 */
package org.nuxeo.ecm.core.api;

import org.nuxeo.ecm.core.event.impl.EventContextImpl;

/**
 * Specialized implementation to be used with an abstract session 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CoreEventContext extends EventContextImpl {
    
    private static final long serialVersionUID = 1L;

    public CoreEventContext(AbstractSession session, Object ... args) {
        super (session, session.getPrincipal(), args);
    }

    public void fireEvent(String name) throws ClientException {
        ((AbstractSession)session).fireEvent(event(name));
    }
    
    public void fireEvent(String name, int flags) throws ClientException {
        ((AbstractSession)session).fireEvent(event(name, flags));
    }
    
}
