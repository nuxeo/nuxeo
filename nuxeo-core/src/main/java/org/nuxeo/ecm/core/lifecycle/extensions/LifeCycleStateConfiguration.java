/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 *
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
        Collection<String> transitions = new ArrayList<String>();
        NodeList elements = element.getElementsByTagName(TAG_TRANSITION);
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element elt = (Element) elements.item(i);
            transitions.add(elt.getTextContent());
        }
        return transitions;
    }

    public Collection<LifeCycleState> getStates() {
        Collection<LifeCycleState> states = new ArrayList<LifeCycleState>();
        NodeList elements = element.getElementsByTagName(TAG_STATE);
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element element = (Element) elements.item(i);
            states.add(new LifeCycleStateImpl(
                    element.getAttribute(ATTR_STATE_NAME),
                    element.getAttribute(ATTR_STATE_DESCRIPTION),
                    getAllowedTransitionsFor(element),
                    Boolean.valueOf(element.getAttribute(ATTR_STATE_INITIAL))));
        }
        return states;
    }

}
