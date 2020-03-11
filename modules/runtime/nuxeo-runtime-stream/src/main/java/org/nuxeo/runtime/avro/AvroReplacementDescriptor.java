/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.runtime.avro;

import java.util.Objects;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * The Avro forbidden character replacement descriptor.<br>
 * Avro allows alphanumeric characters and underscores in names.<br>
 * Nuxeo Studio allows alphanumeric characters, underscores and dashes that have to be replaced by another symbol.<br>
 * <br>
 * The default contributions provide replacement for :<br>
 * - "-" as "__dash__"<br>
 * - ":" as "__colon__<br>
 * - ";" as "__semicolon__"<br>
 * - and with higher priority "__" as "____" to ensure no user string is wrongly replaced.<br>
 *
 * @since 10.2
 */
@XObject("replacement")
public class AvroReplacementDescriptor implements Descriptor, Comparable<AvroReplacementDescriptor> {

    @XNode("@forbidden")
    protected String forbidden;

    @XNode("@replacement")
    protected String replacement;

    @XNode("@priority")
    protected int priority;

    @Override
    public int compareTo(AvroReplacementDescriptor o) {
        return Integer.compare(priority, o.priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AvroReplacementDescriptor other = (AvroReplacementDescriptor) obj;
        return Objects.equals(forbidden, other.forbidden);
    }

    @Override
    public String getId() {
        return forbidden;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (forbidden == null ? 0 : forbidden.hashCode());
        return result;
    }

}
