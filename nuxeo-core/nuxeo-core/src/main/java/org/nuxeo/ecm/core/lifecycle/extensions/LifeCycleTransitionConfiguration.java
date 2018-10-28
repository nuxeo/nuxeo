/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 * $Id: LifeCycleTransitionConfiguration.java 16046 2007-04-12 14:34:58Z fguillaume $
 */

package org.nuxeo.ecm.core.lifecycle.extensions;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;
import org.nuxeo.ecm.core.lifecycle.impl.LifeCycleTransitionImpl;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Configuration helper class for transition.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycleTransition
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LifeCycleTransitionConfiguration {

    private static final String TAG_LIFECYCLE = "lifecycle";

    private static final String TAG_TRANSITIONS = "transitions";

    private static final String TAG_TRANSITION = "transition";

    private static final String ATTR_TRANSITION_NAME = "name";

    private static final String TAG_TRANSITION_DESCRIPTION = "description";

    private static final String ATTR_TRANSITION_DESTINATION_STATE = "destinationState";

    /** The DOM element holding the states. */
    private final Element element;

    public LifeCycleTransitionConfiguration(Element element) {
        this.element = element;
    }

    public Collection<LifeCycleTransition> getTransitions() {
        Collection<LifeCycleTransition> transitions = new ArrayList<LifeCycleTransition>();

        NodeList transitionsElements = element.getElementsByTagName(TAG_TRANSITIONS);
        if (transitionsElements.getLength() <= 0) {
            return transitions;
        }
        // NXP-1472 : don't get the first element, but the first one attached to <lifecycle>
        Element transitionsElement = null;
        for (int i = 0; i < transitionsElements.getLength(); i++) {
            transitionsElement = (Element) transitionsElements.item(i);
            if (TAG_LIFECYCLE.equals(transitionsElement.getParentNode().getNodeName())) {
                break;
            }
        }
        NodeList elements = transitionsElement.getElementsByTagName(TAG_TRANSITION); // NOSONAR
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element element = (Element) elements.item(i);

            String name = element.getAttribute(ATTR_TRANSITION_NAME);
            String destinationState = element.getAttribute(ATTR_TRANSITION_DESTINATION_STATE);
            String description = "";

            if (element.getElementsByTagName(TAG_TRANSITION_DESCRIPTION).getLength() > 0) {
                description = element.getElementsByTagName(TAG_TRANSITION_DESCRIPTION).item(0).getTextContent();
            }
            transitions.add(new LifeCycleTransitionImpl(name, description, destinationState));
        }
        return transitions;
    }

}
