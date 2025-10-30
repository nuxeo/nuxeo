/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.io.marshallers.puml;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * @since 2025.12
 */
@SuppressWarnings("RedundantThrows") // IOException is not always necessary but still interesting for future change
public class PlantUMLPrinter implements Closeable, Flushable {

    protected final PrintWriter writer;

    public PlantUMLPrinter(Writer writer) {
        this.writer = (writer instanceof PrintWriter printWriter) ? printWriter : new PrintWriter(writer);
    }

    public void writeStartDocument() throws IOException {
        writer.println("@startuml");
    }

    public void writeEndDocument() throws IOException {
        writer.println("@enduml");
    }

    public void writeTitle(String title) throws IOException {
        writer.print("title ");
        writer.println(title);
        writer.println();
    }

    public void writeSkinparam(String key, String value) throws IOException {
        writer.print("skinparam ");
        writer.print(key);
        writer.print(" ");
        writer.println(value);
    }

    public void writeSkinparam(String parentKey, String key1, String value1, String key2, String value2)
            throws IOException {
        writer.print("skinparam ");
        writer.print(parentKey);
        writer.println(" {");
        // key1
        writer.print("  ");
        writer.print(key1);
        writer.print(" ");
        writer.println(value1);
        // key2
        writer.print("  ");
        writer.print(key2);
        writer.print(" ");
        writer.println(value2);
        writer.println("}");
    }

    public void writeStartComponent(String identifier) {
        writer.print("component ");
        writer.print(formatIdentifier(identifier));
        writer.println(" [");
    }

    public void writeStartComponent(String identifier, String stereo) {
        writer.print("component ");
        writer.print(formatIdentifier(identifier));
        writer.print(" ");
        writer.print(stereo);
        writer.println("[");
    }

    public void writeEndComponent() throws IOException {
        writer.println("]");
    }

    public void writeStartQueue(String identifier) throws IOException {
        writer.print("queue ");
        writer.print(formatIdentifier(identifier));
        writer.println(" [");
    }

    public void writeEndQueue() throws IOException {
        writer.println("]");
    }

    public void writeArrow(String identifier1, String identifier2) throws IOException {
        writer.print(formatIdentifier(identifier1));
        writer.print("==>");
        writer.println(formatIdentifier(identifier2));
    }

    public void writeArrow(String identifier1, String identifier2, String comment) {
        writer.print(formatIdentifier(identifier1));
        writer.print("==>");
        writer.print(formatIdentifier(identifier2));
        writer.print(": ");
        writer.println(comment);
    }

    public void writeFreeText(String text) throws IOException {
        writer.println(text);
    }

    public void writeTextSeparator() throws IOException {
        writer.println("----");
    }

    protected String formatIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9]", ".");
    }

    protected Writer getWriter() {
        return writer;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }
}
