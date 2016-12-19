/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Razvan Caraghin
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.util;

import java.io.Serializable;

/**
 * Represents a repository location. TODO: move to another package.
 *
 * @author Razvan Caraghin
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class RepositoryLocation implements Serializable, Comparable<RepositoryLocation> {

    private static final long serialVersionUID = -4802281621945117577L;

    protected final String name;

    public RepositoryLocation(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null repository location");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int compareTo(RepositoryLocation o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RepositoryLocation)) {
            return false;
        }
        return name.equals(((RepositoryLocation) other).name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

}
