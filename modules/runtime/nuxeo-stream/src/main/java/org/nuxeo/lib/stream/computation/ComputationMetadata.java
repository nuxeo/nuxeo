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
 *     Taken from https://github.com/concord/concord-jvm
 */
package org.nuxeo.lib.stream.computation;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * The metadata defining a computation.
 *
 * @since 9.3
 */
public class ComputationMetadata {
    protected final String name;

    protected final Set<String> inputStreams;

    protected final Set<String> outputStreams;

    public ComputationMetadata(String name, Set<String> inputStreams, Set<String> outputStreams) {
        this.name = Objects.requireNonNull(name);

        if (inputStreams == null) {
            this.inputStreams = Collections.emptySet();
        } else {
            this.inputStreams = inputStreams;
        }
        if (outputStreams == null) {
            this.outputStreams = Collections.emptySet();
        } else {
            this.outputStreams = outputStreams;
        }

        if (this.inputStreams.isEmpty() && this.outputStreams.isEmpty()) {
            throw new IllegalArgumentException("Both input and output streams are empty");
        }
    }

    /**
     * Globally unique identifier of the computation.
     */
    public String name() {
        return name;
    }

    /**
     * List of streams to subscribe this computation to.
     */
    public Set<String> inputStreams() {
        return inputStreams;
    }

    /**
     * List of streams this computation may produce on.
     */
    public Set<String> outputStreams() {
        return outputStreams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ComputationMetadata metadata = (ComputationMetadata) o;

        return name.equals(metadata.name) && inputStreams.equals(metadata.inputStreams)
                && outputStreams.equals(metadata.outputStreams);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + inputStreams.hashCode();
        result = 31 * result + outputStreams.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ComputationMetadata{" + "name=" + name + ", inputStreams=" + inputStreams + ", outputStreams="
                + outputStreams + '}';
    }
}
