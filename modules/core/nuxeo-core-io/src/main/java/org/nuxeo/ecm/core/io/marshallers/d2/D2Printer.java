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
package org.nuxeo.ecm.core.io.marshallers.d2;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 2025.12
 */
@SuppressWarnings("RedundantThrows") // IOException is not always necessary but still interesting for future change
public class D2Printer implements Closeable, Flushable {

    protected final PrintWriter writer;

    public D2Printer(Writer writer) {
        this.writer = (writer instanceof PrintWriter printWriter) ? printWriter : new PrintWriter(writer);
    }

    public void writeHeader(String header) throws IOException {
        writer.println(header);
    }

    public void writeMarkdownShapeWithClass(String shapeKey, String shapeClass, String content) {
        writeMarkdownShapeWithClass(List.of(shapeKey), shapeClass, content);
    }

    public void writeMarkdownShapeWithClass(Collection<String> shapeKeys, String shapeClass, String content) {
        String identifier = formatIdentifier(shapeKeys);
        writer.println(identifier + ": |md");
        writer.print(content.indent(2));
        if (!content.endsWith("\n")) {
            writer.println();
        }
        writer.println("|");
        writer.println(identifier + ".class: " + shapeClass);
        writer.println();
    }

    public void writeContainerWithClass(String containerKey, String containerLabel, String containerClass) {
        writer.println(formatIdentifier(containerKey) + ": {");
        writer.println("  label: \"%s\"".formatted(containerLabel));
        writer.println("  class: " + containerClass);
        writer.println("}");
        writer.println();
    }

    public void writeConnectionRight(String fromKey, String toKey) {
        writeConnectionRight(List.of(fromKey), List.of(toKey));
    }

    public void writeConnectionRight(String fromKey, String toKey, String label) {
        writeConnectionRight(List.of(fromKey), List.of(toKey), label);
    }

    public void writeConnectionRight(List<String> fromKeys, List<String> toKeys) {
        writer.println("%s -> %s".formatted(formatIdentifier(fromKeys), formatIdentifier(toKeys)));
    }

    public void writeConnectionRight(List<String> fromKeys, List<String> toKeys, String label) {
        writer.println("%s -> %s: %s".formatted(formatIdentifier(fromKeys), formatIdentifier(toKeys), label));
    }

    protected String formatIdentifier(Collection<String> identifiers) {
        return identifiers.stream().map(this::formatIdentifier).collect(Collectors.joining("."));
    }

    protected String formatIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9]", "_");
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
