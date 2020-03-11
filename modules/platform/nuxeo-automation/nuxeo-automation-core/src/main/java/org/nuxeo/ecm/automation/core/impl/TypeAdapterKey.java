/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class TypeAdapterKey {

    public final Class<?> input;

    public final Class<?> output;

    private int hashCode;

    public TypeAdapterKey(Class<?> input, Class<?> output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // this class is final - do not need instanceof check.
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == TypeAdapterKey.class) {
            TypeAdapterKey key = (TypeAdapterKey) obj;
            return key.input == input && key.output == output;
        }
        return false;
    }

    @Override
    public String toString() {
        return input + ":" + output;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = createHashCode();
        }
        return hashCode;
    }

    protected int createHashCode() {
        int result = input.hashCode() | output.hashCode();
        return result == 0 ? 0xbabe : result;
    }

}
