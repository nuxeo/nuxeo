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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.query.sql.model;

import java.io.Serializable;
import java.util.LinkedHashMap;

import com.google.common.collect.Iterables;

/**
 * Here, key is holding the alias and value the document type.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FromList extends LinkedHashMap<String, String> implements Serializable {

    private static final long serialVersionUID = -1931385427413643944L;

    /**
     * Don't use this method anymore. Now we can easily iterate over {@link FromList} with {@link #keySet()},
     * {@link #values()} or {@link #entrySet()}.
     * <p />
     * We kept this method because removing it could lead to regressions as ({@link #get(Object)} is a candidate.
     *
     * @deprecated since 9.1
     */
    @Deprecated
    public String get(int i) {
        return Iterables.get(values(), i);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "";
        }
        return String.join(", ", values());
    }

}
