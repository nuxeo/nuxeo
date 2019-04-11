/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

    private final Map<String, MessageActionDescriptor> actionDescriptorsRegistry = new HashMap<>();

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
            actionDescriptorsRegistry.put(actionName, descriptor.getActions().get(actionName));
        }
        pipe = new MessageActionPipe();
    }

    /**
     * @return initialized action pipe
     */
    public MessageActionPipe getPipe() {
        if (pipe.isEmpty()) {
            fillMissingActionDestination();
            MessageActionDescriptor initialAction = getActions().get(START_ACTION);
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
     * Fill empty destination using registering order to preserve backward compatibility.
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
