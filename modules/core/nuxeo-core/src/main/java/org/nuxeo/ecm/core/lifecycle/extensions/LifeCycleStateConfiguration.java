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
 * $Id: LifeCycleStateConfiguration.java 16207 2007-04-15 11:56:45Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle.extensions;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.impl.LifeCycleStateImpl;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Configuration helper class for state configuration.
 *
 * @see org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleDescriptor
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycleState
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LifeCycleStateConfiguration {

    private static final String TAG_STATE = "state";

    private static final String TAG_TRANSITION = "transition";

    private static final String ATTR_STATE_NAME = "name";

    private static final String ATTR_STATE_DESCRIPTION = "description";

    private static final String ATTR_STATE_INITIAL = "initial";

    /** The DOM element holding the states. */
    private final Element element;

    public LifeCycleStateConfiguration(Element element) {
        this.element = element;
    }

    private static Collection<String> getAllowedTransitionsFor(Element element) {
        Collection<String> transitions = new ArrayList<>();
        NodeList elements = element.getElementsByTagName(TAG_TRANSITION);
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element elt = (Element) elements.item(i);
            transitions.add(elt.getTextContent());
        }
        return transitions;
    }

    public Collection<LifeCycleState> getStates() {
        Collection<LifeCycleState> states = new ArrayList<>();
        NodeList elements = element.getElementsByTagName(TAG_STATE);
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element element = (Element) elements.item(i);
            states.add(new LifeCycleStateImpl(element.getAttribute(ATTR_STATE_NAME),
                    element.getAttribute(ATTR_STATE_DESCRIPTION), getAllowedTransitionsFor(element),
                    Boolean.valueOf(element.getAttribute(ATTR_STATE_INITIAL))));
        }
        return states;
    }

}
