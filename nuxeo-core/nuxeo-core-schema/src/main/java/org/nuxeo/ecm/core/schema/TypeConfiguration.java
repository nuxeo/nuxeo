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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("configuration")
public class TypeConfiguration {

    @XNode("prefetch")
    String prefetchInfo;

    @XNode("clearComplexPropertyBeforeSet")
    Boolean clearComplexPropertyBeforeSet;

    /**
     * @since 10.3
     */
    @XNode("allowVersionWriteForDublinCore")
    protected Boolean allowVersionWriteForDublinCore;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(prefetchInfo=" + prefetchInfo + ", clearComplexPropertyBeforeSet="
                + clearComplexPropertyBeforeSet + ')';
    }

}
