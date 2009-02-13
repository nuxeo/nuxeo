/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
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
        Element transitionsElement=null;
        if (transitionsElements.getLength() > 0) {
            // NXP-1472 : don't get the first element, but the first one attached to <lifecycle>
            for (int i = 0; i < transitionsElements.getLength(); i++) {
                transitionsElement = (Element) transitionsElements.item(i);
                if (TAG_LIFECYCLE.equals(transitionsElement.getParentNode().getNodeName())) {
                    break;
                }
            }
        } else {
            return transitions;
        }
        NodeList elements = transitionsElement.getElementsByTagName(TAG_TRANSITION);
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element element = (Element) elements.item(i);

            String name = element.getAttribute(ATTR_TRANSITION_NAME);
            String destinationState = element
                    .getAttribute(ATTR_TRANSITION_DESTINATION_STATE);
            String description = "";

            if (element.getElementsByTagName(TAG_TRANSITION_DESCRIPTION)
                    .getLength() > 0) {
                description = element.getElementsByTagName(
                        TAG_TRANSITION_DESCRIPTION).item(0).getTextContent();
            }
            transitions.add(new LifeCycleTransitionImpl(name, description,
                    destinationState));
        }
        return transitions;
    }

}
