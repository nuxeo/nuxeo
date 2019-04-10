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

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

/**
 *
 *
 */
public class StreamFeeder {


    public void feed(JsonGenerator generator, JsonParser parser) {
        parser.next(); // skip container start
        while (parser.hasNext()) {
            switch (parser.next()) {
            case START_ARRAY:
                generator.writeStartArray();
                break;
            case END_ARRAY:
                generator.writeEnd();
                break;
            case START_OBJECT:
                generator.writeStartObject();
                break;
            case END_OBJECT:
                if (parser.hasNext()) { // skip container end
                    generator.writeEnd();
                }
                break;
            case KEY_NAME:
                String name = parser.getString();
                switch (parser.next()) {
                case START_ARRAY:
                    generator.writeStartArray(name);
                    break;
                case START_OBJECT:
                    generator.writeStartObject(name);
                    break;
                case VALUE_FALSE:
                    generator.write(name, false);
                    break;
                case VALUE_TRUE:
                    generator.write(name, true);
                    break;
                case VALUE_NULL:
                    generator.writeNull(name);
                    break;
                case VALUE_STRING:
                    generator.write(name, parser.getString());
                    break;
                case VALUE_NUMBER:
                    if (parser.isIntegralNumber()) {
                        generator.write(name, parser.getLong());
                    } else {
                        generator.write(name, parser.getBigDecimal());
                    }
                    break;
                default:
                    throw new UnsupportedOperationException();
                }
                break;
            case VALUE_TRUE:
                generator.write(true);
                break;
            case VALUE_FALSE:
                generator.write(false);
                break;
            case VALUE_NULL:
                generator.writeNull();
                break;
            case VALUE_STRING:
                generator.write(parser.getString());
                break;
            case VALUE_NUMBER:
                if (parser.isIntegralNumber()) {
                    generator.write(parser.getLong());
                } else {
                    generator.write(parser.getBigDecimal());
                }
                break;
            default:
                throw new UnsupportedOperationException();
            }
        }
    }

}
