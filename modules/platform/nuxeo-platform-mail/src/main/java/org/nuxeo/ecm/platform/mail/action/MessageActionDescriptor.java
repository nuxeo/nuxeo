/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            if (action == null || chain != null) {
                return new CreateDocumentsFromAutomationChainAction(chain);
            }
            return action.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not get MessageAction new Instance", e);
        }
    }

}
