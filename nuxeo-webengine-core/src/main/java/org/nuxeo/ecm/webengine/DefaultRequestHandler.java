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

package org.nuxeo.ecm.webengine;

import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.actions.Actions;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultRequestHandler implements RequestHandler, Actions {

    protected DefaultRequestHandler() {
    }

    public void doDelete(SiteObject object) throws SiteException {
        doAction(object, DELETE);
    }

    public void doGet(SiteObject object) throws SiteException {
        if (object.next() == null) {
            doAction(object, VIEW);
        } else {
            doAction(object, null); // avoid doing an action since we have unresolved segments
        }
    }

    public void doHead(SiteObject object) throws SiteException {
        doGet(object);
    }

    public void doPost(SiteObject object) throws SiteException {
        if (object.next() == null) { // this is the last object -> the default action is update
            doAction(object, UPDATE);
        } else if (object.getDocument().isFolder()) {
            doAction(object, CREATE);
        } else {
            doAction(object, UPDATE);
        }
    }

    public void doPut(SiteObject object) throws SiteException {
        doPost(object);
    }

    public boolean traverse(SiteObject object) throws SiteException {
        return true;
    }

    public static void doAction(SiteObject object, String defaultAction) throws SiteException {
        String action = object.getSiteRequest().getAction();
        if (action == null) {
            action = defaultAction;
            object.getSiteRequest().setAction(action);
        }
        if (action == null) {
            return;
        }
        ActionDescriptor actionDesc = object.getAction(action);
        if (actionDesc != null) {
            try {
                actionDesc.run(object);
            } catch (SiteException e) {
                throw e;
            } catch (Exception e) {
                throw new SiteException("Failed to run action "+action+" on object "+object.getPath(), e);
            }
        }
    }

}
