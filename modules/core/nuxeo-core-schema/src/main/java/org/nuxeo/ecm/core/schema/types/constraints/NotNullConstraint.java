/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This constraint ensure some object is not null.
 * <p>
 * This class is a singleton. Use {@link #get()} to get the singleton.
 * </p>
 *
 * @since 7.1
 * @author <a href="mailto:nc@nuxeo.com">Nicolas Chapurlat</a>
 */
public class NotNullConstraint extends AbstractConstraint {

    private static final long serialVersionUID = 1L;

    private static final String NAME = "NotNullConstraint";

    // Lazy ang thread safe singleton instance.
    private static class Holder {
        private static final NotNullConstraint INSTANCE = new NotNullConstraint();
    }

    private NotNullConstraint() {
    }

    public static NotNullConstraint get() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean validate(Object object) {
        return object != null;
    }

    /**
     * Here, value is : <br>
     * name = {@value #NAME}. <br>
     * parameters is empty
     */
    @Override
    public Description getDescription() {
        Map<String, Serializable> params = new HashMap<>();
        return new Description(NotNullConstraint.NAME, params);
    }

    @Override
    public int hashCode() {
        return NotNullConstraint.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof NotNullConstraint;
    }
}
