/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.kafka;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventBundleImpl;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.ReconnectedEventBundleImpl;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.event.impl.ShallowEvent;

/**
 * @since 8.4
 */
public class EventBundleJSONIO {

    protected static JsonFactory jsonFactory = null;

    protected static JsonFactory getJsonFactory() {
        if (jsonFactory == null) {
            jsonFactory = new JsonFactory(new ObjectMapper());
        }
        return jsonFactory;
    }

    public String marshall(EventBundle events) {

        StringWriter writer = new StringWriter();

        try {
            JsonGenerator jg = getJsonFactory().createJsonGenerator(writer);
            jg.writeStartArray();
            for (Event event : events) {
                writeEvent(jg, event);
            }
            jg.writeEndArray();
            jg.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public EventBundle unmarshal(String message) {

        try {
            JsonParser jp = getJsonFactory().createJsonParser(new StringReader(message));

            JsonNode jn = jp.readValueAsTree();

            Iterator<JsonNode> it = jn.getElements();

            EventBundleImpl bundle = new EventBundleImpl();
            while (it.hasNext()) {
                bundle.push(readEvent(it.next()));
            }

            return new ReconnectedEventBundleImpl(bundle);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Event readEvent(JsonNode eventNode) throws JsonProcessingException, IOException {
        String eventName = eventNode.get("eventName").getValueAsText();
        long dateTime = eventNode.get("dateTime").getLongValue();
        int flags = eventNode.get("flags").getIntValue();

        EventContext ctx=readEventContext(eventNode.get("context"));

        return new ShallowEvent(eventName, ctx, flags, dateTime);
    }

    protected void writeEvent(JsonGenerator jg, Event event) throws JsonGenerationException, IOException {

        if (!(event instanceof ShallowEvent)) {
            event = ShallowEvent.create(event);
        }

        jg.writeStartObject();

        jg.writeStringField("eventName", event.getName());
        jg.writeNumberField("dateTime", event.getTime());
        jg.writeNumberField("flags", event.getFlags());

        EventContext ctx = event.getContext();
        writeEventContext(jg, ctx);

        jg.writeEndObject();

    }

    protected void writeEventContext(JsonGenerator jg, EventContext ctx) throws JsonGenerationException, IOException  {
        jg.writeObjectFieldStart("context");
        jg.writeStringField("type", ctx.getClass().getName());

        jg.writeArrayFieldStart("args");
        for (Object arg : ctx.getArguments()) {
            writeObject(jg, arg);
        }
        jg.writeEndArray();

        jg.writeStringField("principal", ctx.getPrincipal().getName());
        jg.writeStringField("repository", ctx.getRepositoryName());

        Map<String, Serializable> props = ctx.getProperties();
        jg.writeObjectFieldStart("props");
        for (String k : props.keySet()) {
            jg.writeFieldName(k);
            writeObject(jg, props.get(k));
        }
        jg.writeEndObject();
        jg.writeEndObject();

    }

    protected EventContext readEventContext (JsonNode ctxNode) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();

        Principal principal = new SimplePrincipal(ctxNode.get("principal").getValueAsText());
        String repository = ctxNode.get("repository").getValueAsText();

        String type = ctxNode.get("type").getValueAsText();


        JsonNode argsNode = ctxNode.get("args");
        List<Object> args = new ArrayList<Object>();
        for (JsonNode arg : argsNode) {
            args.add(readObject(arg, mapper));
        }

        EventContext ctx=null;
        if (type.equals("org.nuxeo.ecm.core.event.impl.DocumentEventContext")) {
            ctx = new DocumentEventContext((CoreSession) null, principal, (DocumentModel) args.get(0));
        } else {
            ctx = new EventContextImpl(args);
        }


        Map<String, Serializable> props = new HashMap<String, Serializable>();
        JsonNode propsNode = ctxNode.get("props");
        for (JsonNode propNode : propsNode) {

        }

        ctx.setProperties(props);
        return ctx;
    }


    protected Object readObject(JsonNode node, ObjectMapper mapper) throws JsonProcessingException, IOException {

        if (node.has("type") && node.has("path") && node.has("isFolder")) {
            Map<String, Serializable> ctxMap = mapper.convertValue(node, Map.class);
            ScopedMap smap = new ScopedMap();
            smap.putAll(ctxMap);

            JsonNode facetsNode = node.get("facets");
            List<String> lfacets = new ArrayList<String>();
            for (JsonNode facet : facetsNode) {
                lfacets.add(facet.getTextValue());
            }

            Set<String> facets = new HashSet<String>();
            facets.addAll(lfacets);

            return new ShallowDocumentModel(node.get("id").getTextValue(), node.get("repoName").getTextValue(), node.get("name").getTextValue(), new Path(node.get("path").getTextValue()), node.get("type").getTextValue(), node.get("isFolder").getBooleanValue(),
                    node.get("isVersion").getBooleanValue(), node.get("isproxy").getBooleanValue(), node.get("isImmutable").getBooleanValue(), smap, facets,
                    node.get("lifeCycleState").getTextValue());
        }
        return mapper.reader().readValue(node);
    }


    protected void writeObject(JsonGenerator jg, Object object) throws JsonGenerationException, IOException {
        if (object instanceof ShallowDocumentModel) {
            ShallowDocumentModel doc = (ShallowDocumentModel) object;
            jg.writeStartObject();

            jg.writeStringField("id",doc.getId());
            jg.writeStringField("repoName",doc.getRepositoryName());
            jg.writeStringField("name" , doc.getName());
            jg.writeStringField("path", doc.getPathAsString());
            jg.writeStringField("type", doc.getType());
            jg.writeBooleanField("isFolder", doc.isFolder());
            jg.writeBooleanField("isVersion",doc.isVersion());
            jg.writeBooleanField("isProxy", doc.isProxy());
            jg.writeBooleanField("isImmutable", doc.isImmutable());
            jg.writeStringField("lifecycleState", doc.getCurrentLifeCycleState());

            jg.writeArrayFieldStart("facets");
            for (String facet : doc.getFacets()) {
                jg.writeString(facet);
            }
            jg.writeEndArray();

            jg.writeObjectFieldStart("context");
            for (String k : doc.getContextData().keySet()) {
                jg.writeObjectField(k, doc.getContextData().get(k));
            }
            jg.writeEndObject();

            jg.writeEndObject();

        } else {
            jg.writeObject(object);
        }
    }

}
