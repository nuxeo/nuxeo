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

package org.nuxeo.ecm.webengine.rest.model;

import java.util.Set;

import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.security.Guard;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ActionType extends WebResourceType<WebAction> {
    
    Set<String> getCategories();
    
    boolean isEnabled();
    
    boolean isEnabled(WebContext2 ctx);
    
    /**
     * TODO use guard into WebType too?
     * @return
     */
    Guard getGuard();
    
}
