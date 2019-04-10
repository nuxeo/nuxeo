/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.tools.report.client;

import java.util.Stack;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 *
 *
 */
public class ObjectFeeder {
    interface Handler {
        void handle(JsonArrayBuilder builder);

        void handle(JsonObjectBuilder builder);

        void handle(Event current, JsonParser parser);
    }

    class ArrayHandler implements Handler {
        final JsonArrayBuilder builder;

        ArrayHandler(JsonArrayBuilder aBuilder) {
            builder = aBuilder;
        }

        @Override
        public void handle(Event event, JsonParser parser) {
            switch (event) {
            case START_ARRAY:
                stack.push(new ArrayHandler(Json.createArrayBuilder()));
                break;
            case START_OBJECT:
                stack.push(new ObjectHandler(Json.createObjectBuilder()));
                break;
            case END_ARRAY:
                stack.pop();
                stack.peek().handle(builder);
                break;
            case END_OBJECT:
                stack.pop();
                stack.peek().handle(builder);
                break;
            case VALUE_FALSE:
                builder.add(false);
                break;
            case VALUE_TRUE:
                builder.add(true);
                break;
            case VALUE_STRING:
                builder.add(parser.getString());
                break;
            case VALUE_NULL:
                builder.addNull();
                break;
            case VALUE_NUMBER:
                if (parser.isIntegralNumber()) {
                    builder.add(parser.getLong());
                } else {
                    builder.add(parser.getBigDecimal());
                }
                break;
            default:
                throw new IllegalStateException("Cannot handle " + event + " while parsing object");
            }
        }

        @Override
        public void handle(JsonArrayBuilder builder) {
            this.builder.add(builder);
        }

        @Override
        public void handle(JsonObjectBuilder builder) {
            this.builder.add(builder);
        }
    }

    class ObjectHandler implements Handler {
        final JsonObjectBuilder builder;

        ObjectHandler(JsonObjectBuilder aBuilder) {
            builder = aBuilder;
        }

        String name;

        @Override
        public void handle(Event anEvent, JsonParser aParser) {
            switch (anEvent) {
            case END_OBJECT:
                stack.pop();
                if (!stack.isEmpty()) {
                    stack.peek().handle(builder);
                }
                break;
            case KEY_NAME:
                name = aParser.getString();
                switch (aParser.next()) {
                case START_ARRAY:
                    stack.push(new ArrayHandler(Json.createArrayBuilder()));
                    break;
                case START_OBJECT:
                    stack.push(new ObjectHandler(Json.createObjectBuilder()));
                    break;
                case VALUE_FALSE:
                    builder.add(name, false);
                    break;
                case VALUE_TRUE:
                    builder.add(name, true);
                    break;
                case VALUE_NULL:
                    builder.addNull(name);
                    break;
                case VALUE_STRING:
                    builder.add(name, aParser.getString());
                    break;
                case VALUE_NUMBER:
                    if (aParser.isIntegralNumber()) {
                        builder.add(name, aParser.getLong());
                    } else {
                        builder.add(name, aParser.getBigDecimal());
                    }
                    break;
                default:
                    throw new IllegalStateException("Cannot handle " + anEvent + " while parsing object");
                }
                break;
            default:
                throw new IllegalStateException("Cannot handle " + anEvent + " while parsing object");
            }
        }

        @Override
        public void handle(JsonArrayBuilder builder) {
            this.builder.add(name, builder);
        }

        @Override
        public void handle(JsonObjectBuilder builder) {
            this.builder.add(name, builder);
        }

    }

    final Stack<Handler> stack = new Stack<>();

    public JsonArrayBuilder buildArray(JsonArrayBuilder builder, JsonParser parser) {
        if (!parser.hasNext()) {
            throw new JsonException("not an array");
        }
        if (parser.next() != Event.START_ARRAY) {
            throw new IllegalStateException("Not an array");
        }
        stack.push(new ArrayHandler(builder));
        try {
            dofeed(parser);
        } finally {
            stack.clear();
        }
        return builder;
    }

    public JsonObjectBuilder feed(JsonObjectBuilder builder, JsonParser parser) {
        if (!parser.hasNext()) {
            throw new JsonException("not an array");
        }
        if (parser.next() != Event.START_OBJECT) {
            throw new IllegalStateException("Not an array");
        }
        stack.push(new ObjectHandler(builder));
        try {
            dofeed(parser);
        } finally {
            stack.clear();
        }
        return builder;
    }

    void dofeed(JsonParser parser) {
        while (parser.hasNext()) {
            stack.peek().handle(parser.next(), parser);
        }
    }
}
