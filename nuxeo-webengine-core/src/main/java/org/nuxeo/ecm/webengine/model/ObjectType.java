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

package org.nuxeo.ecm.webengine.model;

import org.nuxeo.ecm.webengine.model.impl.ActionTypeImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ObjectType extends ResourceType<ObjectResource> {
    
    public final static String ROOT_TYPE_NAME = "*";

    public ObjectType getSuperType();
    public Class<? extends ObjectResource> getObjectType();
    public ActionTypeImpl getAction(String name);
    public ActionTypeImpl addAction(ActionTypeImpl action);
    public void removeAction(String name);
    public ActionTypeImpl[] getActions();
    public ActionTypeImpl[] getActions(String category);
    public ActionTypeImpl[] getEnabledActions(WebContext ctx);
    public ActionTypeImpl[] getEnabledActions(String category, WebContext ctx);
    public ActionTypeImpl[] getLocalActions();
    
}
