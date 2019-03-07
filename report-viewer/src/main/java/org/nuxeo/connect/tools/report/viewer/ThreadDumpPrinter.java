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
package org.nuxeo.connect.tools.report.viewer;

import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.util.Iterator;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

import org.jolokia.converter.Converters;
import org.jolokia.converter.object.StringToOpenTypeConverter;
import org.nuxeo.runtime.management.jvm.ThreadDeadlocksDetector.JVM16Printer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import sun.management.MappedMXBeanType;

/**
 * Prints thread dumps included in the report.
 *
 * @since 8.4
 */
public class ThreadDumpPrinter {

    final ArrayNode dump;

    ThreadDumpPrinter(ArrayNode dump) {
        this.dump = dump;
    }

    public Iterable<ThreadInfo> iterableOf() {
        return () -> {
            try {
                return iteratorOf();
            } catch (IOException cause) {
                throw new AssertionError("Cannot parse thread dump", cause);
            }
        };
    }

    final StringToOpenTypeConverter converter = new Converters().getToOpenTypeConverter();

    public Iterator<ThreadInfo> iteratorOf() throws IOException {

        return new Iterator<>() {

            Iterator<JsonNode> nodes = dump.iterator();

            @Override
            public boolean hasNext() {
                return nodes.hasNext();
            }

            @Override
            public ThreadInfo next() {
                try {
                    return ThreadInfo.from((CompositeData) converter.convertToObject(
                            MappedMXBeanType.toOpenType(ThreadInfo.class), nodes.next().toString()));
                } catch (OpenDataException cause) {
                    throw new AssertionError("Cannot parse thread info attributes", cause);
                }
            }

        };
    }

    public StringBuilder print(StringBuilder sb) {
        JVM16Printer printer = new JVM16Printer();
        for (ThreadInfo ti : iterableOf()) {
            printer.print(sb, ti);
        }
        return sb;
    }

}
