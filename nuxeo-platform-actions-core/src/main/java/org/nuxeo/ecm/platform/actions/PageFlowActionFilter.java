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
 *
 * $Id: PageFlowActionFilter.java 21461 2007-06-26 20:42:26Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.util.Collections;
import java.util.Map;

import org.jboss.seam.pageflow.Pageflow;
import org.jbpm.graph.def.Transition;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// Not used?
public class PageFlowActionFilter extends AbstractActionFilter {

    public static final Map<String, Transition> EMPTY_TRANSITION_MAP = Collections.emptyMap();

    private static final long serialVersionUID = 3003327715065315406L;

    private Map<String, Transition> pageFlowTransitions;

    public PageFlowActionFilter() {
        super("PAGE_FLOW_FILTER", Action.EMPTY_CATEGORIES);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Transition> getPageflowTransitions() {
        if (pageFlowTransitions == null) {
            try {
                pageFlowTransitions = Pageflow.instance()
                    .getPage().getLeavingTransitionsMap();
            } catch (IllegalStateException e) {
                // no seam context found
                pageFlowTransitions = EMPTY_TRANSITION_MAP;
            }
        }
        return pageFlowTransitions;
    }

    public boolean accept(Action action, ActionContext context) {
        Map<String, Transition> leavingTransitions = getPageflowTransitions();
        return leavingTransitions.containsKey(action.getId());
    }

}
