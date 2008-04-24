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

package org.nuxeo.ecm.platform.site.actions;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.site.SiteException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("action")
public class ActionDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@script")
    protected String script;

    @XNode("@handler")
    protected Class<ActionHandler> handlerClass;

    @XNode("@enabled")
    protected boolean isEnabled;

    //TODO
    protected String[] permissions;

    protected ActionHandler handler;


    public ActionDescriptor() {
        // TODO Auto-generated constructor stub
    }

    public ActionDescriptor(String id, String path, Class<ActionHandler> handler, String[] perms) {
        this.id = id;
        this.script = path;
        this.handlerClass = handler;
        this.permissions = perms;
    }

    /**
     * @param isEnabled the isEnabled to set.
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * @return the isEnabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the handler.
     */
    public Class<ActionHandler> getHandlerClass() {
        return handlerClass;
    }

    /**
     * @param handlerClass the handlerClass to set.
     */
    public void setHandlerClass(Class<ActionHandler> handlerClass) {
        this.handlerClass = handlerClass;
    }

    /**
     * @return the path.
     */
    public String getScript() {
        return script;
    }

    /**
     * @param path the path to set.
     */
    public void setPath(String path) {
        this.script = path;
    }

    /**
     * @return the permissions.
     */
    public String[] getPermissions() {
        return permissions;
    }

    /**
     * @param permissions the permissions to set.
     */
    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    @XNode("@permissions")
    public void setPermissionsFromString(String permissions) {
        this.permissions = StringUtils.split(permissions, '|', true);
    }

    public ActionHandler getHandler() throws SiteException {
        if (handler == null) {
            if (handlerClass == null) {
                handler = ActionHandler.NULL;
            } else {
                try {
                    handler = handlerClass.newInstance();
                } catch (Exception e) {
                    throw new SiteException("Failed to instantiate action handler for action: "+id, e);
                }
            }
        }
        return handler;
    }

    public void setHandler(ActionHandler handler) {
        this.handler = handler;
    }

    public void merge(ActionDescriptor ad) {
        if (script == null) {
            this.script = ad.script;
        }
        if (handlerClass == null) {
            handlerClass = ad.handlerClass;
        }
        if (permissions == null) {
            permissions = ad.permissions;
        } else if (permissions.length > 0) {
            if (".".equals(permissions[0])) { // append current permissions to the one defined in the base action
                if (ad.permissions == null || ad.permissions.length == 0) {
                    String[] tmp = new String[permissions.length-1];
                    System.arraycopy(permissions, 1, tmp, 0, tmp.length);
                    permissions = tmp;
                } else {
                    String[] tmp = new String[permissions.length+ad.permissions.length];
                    System.arraycopy(ad.permissions, 0, tmp, 0, ad.permissions.length);
                    System.arraycopy(permissions, 1, tmp, ad.permissions.length, permissions.length-1);
                    permissions = tmp;
                }
            }
        }
    }

}
