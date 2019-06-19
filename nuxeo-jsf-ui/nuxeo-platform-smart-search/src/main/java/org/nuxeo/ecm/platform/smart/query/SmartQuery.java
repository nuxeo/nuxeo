/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.query;

import java.io.Serializable;

/**
 * Common interface for query build
 *
 * @since 5.4
 * @author Anahide Tchertchian
 */
public interface SmartQuery extends Serializable {

    /**
     * Returns a String with the built query (or query part).
     */
    String buildQuery();

    /**
     * Returns true if query is valid.
     */
    boolean isValid();

}
