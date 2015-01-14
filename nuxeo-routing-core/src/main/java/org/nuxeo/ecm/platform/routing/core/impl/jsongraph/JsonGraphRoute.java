/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.impl.jsongraph;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;

/**
 * @since 7.2
 */
public class JsonGraphRoute extends UnrestrictedSessionRunner {

    public static String getI18nLabel(String label, Locale locale) {
        if (label == null) {
            label = "";
        }
        return I18NUtils.getMessageString("messages", label, null, locale);
    }

    public static String toJSON(GraphRoute route, Locale locale) {
        try {
            Map<String, Object> graph = new HashMap<String, Object>();
            List<NodeView> nodeViews = new ArrayList<NodeView>();
            Map<String, TransitionView> tranViews = new HashMap<String, TransitionView>();

            for (GraphNode node : route.getNodes()) {
                nodeViews.add(new NodeView(node, locale));
                List<Transition> transitions = node.getOutputTransitions();
                for (Transition transition : transitions) {
                    GraphNode targetNode = route.getNode(transition.getTarget());
                    tranViews.put(transition.getId(),
                            new TransitionView(node.getId(), targetNode.getId(), transition.getLabel(), locale));
                }
            }
            graph.put("nodes", nodeViews);
            graph.put("transitions", tranViews.values());

            ObjectMapper mapper = new ObjectMapper();
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, graph);
            return writer.toString();
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected String docId;

    protected GraphRoute graphRoute;

    protected String json;

    protected Locale locale;

    public JsonGraphRoute(CoreSession session, String docId, Locale locale) {
        super(session);
        this.docId = docId;
        this.locale = locale;
    }

    public JsonGraphRoute(CoreSession session, GraphRoute graphRoute, Locale locale) {
        super(session);
        this.graphRoute = graphRoute;
        this.locale = locale;
    }

    public String getJSON() throws ClientException {
        runUnrestricted();
        return json;
    }

    @Override
    public void run() throws ClientException {
        if (graphRoute == null) {
            DocumentModel doc = session.getDocument(new IdRef(docId));
            graphRoute = doc.getAdapter(GraphRoute.class);
        }
        json = toJSON(graphRoute, locale);
    }

    @Override
    public String toString() {
        return getJSON();
    }
}