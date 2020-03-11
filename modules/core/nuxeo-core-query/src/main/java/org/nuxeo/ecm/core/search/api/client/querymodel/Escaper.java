/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.search.api.client.querymodel;

/**
 * An interface for special character escaping in queries for easy configuration of this matter.
 * <p>
 * This is meant for the contents of string literals in where clauses, once they have been extracted. It's therefore not
 * necessary to escape single quotes, unless of course they have some meaning to the search backend.
 * </p>
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public interface Escaper {

    /**
     * Escapes the provided string
     *
     * @param s
     * @return the escaped string
     */
    String escape(String s);
}
