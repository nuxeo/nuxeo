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

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.impl.ActionDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebType {
    
    public final static String ROOT_TYPE_NAME = "*";

    public WebType getSuperType();
    public Class<? extends WebObject> getObjectType();
    public String getName();
    public WebObject newInstance() throws WebException;
    public ActionDescriptor getAction(String name);
    public WebAction getActionInstance(WebContext2 ctx, String name) throws WebException;
    public ActionDescriptor addAction(ActionDescriptor action);
    public void removeAction(String name);
    public ActionDescriptor[] getActions();
    public ActionDescriptor[] getActions(String category);
    public ActionDescriptor[] getEnabledActions(WebContext2 ctx);
    public ActionDescriptor[] getEnabledActions(String category, WebContext2 ctx);
    public ActionDescriptor[] getLocalActions();
    
}
