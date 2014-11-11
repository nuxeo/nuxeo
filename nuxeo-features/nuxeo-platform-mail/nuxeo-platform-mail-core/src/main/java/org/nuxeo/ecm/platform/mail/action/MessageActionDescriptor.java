/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.mail.listener.action.CreateDocumentsFromAutomationChainAction;

/**
 * @author Laurent Doguin
 */
@XObject("action")
public class MessageActionDescriptor {

    @XNode("@id")
    private String id;

    @XNode("@to")
    private String to;

    @XNode("@chain")
    private String chain;
    
    @XNode
    Class<? extends MessageAction> action;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getId() {
        if (id == null || "".equals(id)) {
            id = action.getSimpleName();
        }
        return id;
    }

    public MessageAction getAction() {
        try {
            if (action==null || chain!=null) {
                return new CreateDocumentsFromAutomationChainAction(chain);
            }
            return action.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(
                    "Could not get MessageAction new Instance", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
