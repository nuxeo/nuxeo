/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.tests.computation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.Name;

/**
 * Computation that read from multiple inputs and round robin records on outputs. Request for checkpoint on each record
 * (no batching).
 *
 * @since 9.3
 */
public class ComputationForward extends AbstractComputation {
    protected final List<String> ostreamList;

    protected int counter = 0;

    public ComputationForward(String name, int inputs, int outputs) {
        super(name, inputs, outputs);
        if (inputs <= 0) {
            throw new IllegalArgumentException("Cannot forward without inputs");
        }
        ostreamList = new ArrayList<>(metadata.outputStreams());
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        // dispatch record to output stream
        String outputStream = ostreamList.get(counter++ % ostreamList.size());
        context.produceRecord(outputStream, record);
        context.askForCheckpoint();
    }

}
