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

    public void doDelete(WebObject object) throws WebException {
        doAction(object, DELETE);
    }

    public void doGet(WebObject object) throws WebException {
        if (object.next() == null) {
            doAction(object, VIEW);
        } else {
            doAction(object, null); // avoid doing an action since we have unresolved segments
        }
    }

    public void doHead(WebObject object) throws WebException {
        doGet(object);
    }

    public void doPost(WebObject object) throws WebException {
        if (object.next() == null) { // this is the last object -> the default action is update
            doAction(object, UPDATE);
        } else if (object.getDocument().isFolder()) {
            doAction(object, CREATE);
        } else {
            doAction(object, UPDATE);
        }
    }

    public void doPut(WebObject object) throws WebException {
        doPost(object);
    }

    public boolean traverse(WebObject object) throws WebException {
        return true;
    }

    public static void doAction(WebObject object, String defaultAction) throws WebException {
        String action = object.getWebContext().getActionName();
        if (action == null) {
            action = defaultAction;
            object.getWebContext().setActionName(action);
        }
        if (action == null) {
            return;
        }
        ActionDescriptor actionDesc = object.getAction(action);
        if (actionDesc != null) {
            try {
                actionDesc.run(object);
            } catch (WebException e) {
                throw e;
            } catch (Exception e) {
                throw new WebException("Failed to run action "+action+" on object "+object.getObjectPath(), e);
            }
        }
    }

}
