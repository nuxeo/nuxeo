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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Alexandre Russel
 * @author Laurent Doguin
 */
@XObject("pipe")
public class MessageActionPipeDescriptor {

    private static final String START_ACTION = "StartAction";

    @XNode("@name")
    private String name;

    @XNode("@override")
    private Boolean override;

    private final Map<String, MessageActionDescriptor> actionDescriptorsRegistry = new HashMap<String, MessageActionDescriptor>();

    private MessageActionPipe pipe = new MessageActionPipe();

    @XNodeList(value = "action", type = MessageActionDescriptor[].class, componentType = MessageActionDescriptor.class)
    MessageActionDescriptor[] actions;

    public String getName() {
        return name;
    }

    public boolean getOverride() {
        if (override == null) {
            return false;
        }
        return override;
    }

    public Map<String, MessageActionDescriptor> getActions() {
        for (MessageActionDescriptor action : actions) {
            actionDescriptorsRegistry.put(action.getId(), action);
        }
        return actionDescriptorsRegistry;
    }

    /**
     * Merge this MessageActionPipeDescriptor with the given one.
     */
    public void merge(MessageActionPipeDescriptor descriptor) {
        for (String actionName : descriptor.getActions().keySet()) {
            actionDescriptorsRegistry.put(actionName,
                    descriptor.getActions().get(actionName));
        }
        pipe = new MessageActionPipe();
    }

    /**
     * @return initialized action pipe
     */
    public MessageActionPipe getPipe() {
        if (pipe.isEmpty()) {
            fillMissingActionDestination();
            MessageActionDescriptor initialAction = getActions().get(
                    START_ACTION);
            addAction(initialAction);
        }
        return pipe;
    }

    private void addAction(MessageActionDescriptor msgActionDescriptor) {
        pipe.add(msgActionDescriptor.getAction());
        String next = msgActionDescriptor.getTo();
        MessageActionDescriptor nextAction = actionDescriptorsRegistry.get(next);
        if (nextAction == null) {
            return;
        } else {
            addAction(nextAction);
        }
    }

    /**
     * Fill empty destination using registering order to preserve backward
     * compatibility.
     */
    private void fillMissingActionDestination() {
        for (int i = 0; i < actions.length - 1; i++) {
            String destination = actions[i].getTo();
            if (destination == null || "".equals(destination)) {
                String newDestination = actions[i + 1].getId();
                if (newDestination != null) {
                    actions[i].setTo(newDestination);
                }
            }
        }
    }
}
