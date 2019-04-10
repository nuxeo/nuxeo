/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.impl.jsongraph;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;

/**
 * @since 7.2
 */
public class JsonGraphRoute extends UnrestrictedSessionRunner {

    public static Map<String, Object> getGraphElementsAsMap(GraphRoute route, Locale locale) {
        Map<String, Object> graph = new HashMap<String, Object>();
        List<NodeView> nodeViews = new ArrayList<NodeView>();
        List<TransitionView> tranViews = new ArrayList<TransitionView>();

        for (GraphNode node : route.getNodes()) {
            nodeViews.add(new NodeView(node, locale));
            List<Transition> transitions = node.getOutputTransitions();
            for (Transition transition : transitions) {
                GraphNode targetNode = route.getNode(transition.getTarget());
                tranViews.add(new TransitionView(node.getId(), targetNode.getId(), transition, locale));
            }
        }
        graph.put("nodes", nodeViews);
        graph.put("transitions", tranViews);
        return graph;
    }

    public static String getI18nLabel(String label, Locale locale) {
        if (label == null) {
            label = "";
        }
        try {
            return I18NUtils.getMessageString("messages", label, null, locale);
        } catch (MissingResourceException e) {
            log.warn(e.getMessage());
            return label;
        }
    }

    private static Log log = LogFactory.getLog(JsonGraphRoute.class);

    protected String docId;

    protected GraphRoute graphRoute;

    protected Map<String, Object> graphElements;

    protected String json;

    protected Locale locale;

    public JsonGraphRoute(CoreSession session, GraphRoute graphRoute, Locale locale) {
        super(session);
        this.graphRoute = graphRoute;
        this.locale = locale;
    }

    public JsonGraphRoute(CoreSession session, String docId, Locale locale) {
        super(session);
        this.docId = docId;
        this.locale = locale;
    }

    /**
     * @since 7.2
     */
    public Map<String, Object> getGraphElements() {
        if (graphElements == null) {
            runUnrestricted();
        }
        return graphElements;
    }

    public String getJSON() {
        if (json == null) {
            runUnrestricted();
        }
        return json;
    }

    @Override
    public void run() {
        if (graphRoute == null) {
            DocumentModel doc = session.getDocument(new IdRef(docId));
            graphRoute = doc.getAdapter(GraphRoute.class);
        }
        try {
            graphElements = getGraphElementsAsMap(graphRoute, locale);
            ObjectMapper mapper = new ObjectMapper();
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, graphElements);
            json = writer.toString();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public String toString() {
        return getJSON();
    }
}
