/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.mail.action;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * @author Alexandre Russel
 * @author Laurent Doguin
 */
@XObject("pipe")
@XRegistry
public class MessageActionPipeDescriptor {

    private static final String START_ACTION = "StartAction";

    protected static final Collector<MessageActionDescriptor, ?, Map<String, MessageActionDescriptor>> COLLECTOR_TO_ID_DESC_MAP = Collectors.toMap(
            MessageActionDescriptor::getId, Function.identity(), (e1, e2) -> e2, LinkedHashMap::new);

    @XNode("@name")
    @XRegistryId
    private String name;

    /**
     * @deprecated since 11.5: should use {@link XMerge#MERGE} instead
     */
    @Deprecated(since = "11.5")
    @XNode("@override")
    public boolean override;

    // initialized trough setter
    @XNodeList(value = "action", type = MessageActionDescriptor[].class, componentType = MessageActionDescriptor.class)
    private MessageActionDescriptor[] actions;

    public String getName() {
        return name;
    }

    /**
     * @deprecated since 11.5, only used for internal {@link #getPipe()}.
     */
    @Deprecated(since = "11.5")
    public Map<String, MessageActionDescriptor> getActions() {
        return getActionsMap();
    }

    public Map<String, MessageActionDescriptor> getActionsMap() {
        return Stream.of(actions).collect(COLLECTOR_TO_ID_DESC_MAP);
    }

    /**
     * @return initialized action pipe
     */
    public MessageActionPipe getPipe() {
        MessageActionPipe pipe = new MessageActionPipe();
        Map<String, MessageActionDescriptor> actionsMap = getActionsMap();
        MessageActionDescriptor initialAction = actionsMap.get(START_ACTION);
        addAction(pipe, actionsMap, initialAction);
        return pipe;
    }

    private MessageActionPipe addAction(MessageActionPipe pipe, Map<String, MessageActionDescriptor> actionsMap,
            MessageActionDescriptor msgActionDescriptor) {
        pipe.add(msgActionDescriptor.getAction());
        String next = msgActionDescriptor.getTo();
        MessageActionDescriptor nextAction = actionsMap.get(next);
        if (nextAction == null) {
            return pipe;
        } else {
            return addAction(pipe, actionsMap, nextAction);
        }
    }

    /**
     * Fill empty destination using registering order to preserve backward compatibility.
     * <p>
     * Method made public starting from 11.5 so that this is done at first registration.
     *
     * @since 11.5
     */
    public void fillMissingActionDestination() {
        for (int i = 0; i < actions.length - 1; i++) {
            MessageActionDescriptor desc = actions[i];
            String destination = desc.getTo();
            if (StringUtils.isBlank(destination)) {
                String newDestination = actions[i + 1].getId();
                if (newDestination != null) {
                    desc.setTo(newDestination);
                }
            }
        }
    }
}
