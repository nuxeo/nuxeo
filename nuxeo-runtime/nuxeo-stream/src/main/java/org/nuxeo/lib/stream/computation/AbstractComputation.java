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
package org.nuxeo.lib.stream.computation;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An abstract {@link Computation} that manages the metadata init.<br/>
 * By convention the inputs streams are named internally: i1, i2 ...<br/>
 * and the output streams are named: o1, o2 ...
 *
 * @since 9.3
 */
public abstract class AbstractComputation implements Computation {
    protected final ComputationMetadata metadata;

    public AbstractComputation(String name, int nbInputStreams, int nbOutputStreams) {
        this.metadata = new ComputationMetadata(name,
                IntStream.range(1, nbInputStreams + 1).boxed().map(i -> "i" + i).collect(Collectors.toSet()),
                IntStream.range(1, nbOutputStreams + 1).boxed().map(i -> "o" + i).collect(Collectors.toSet()));
    }

    @Override
    public void init(ComputationContext context) {

    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {

    }

    @Override
    public ComputationMetadata metadata() {
        return metadata;
    }
}
