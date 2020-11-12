/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime.mongodb;

/**
 * @since 11.4
 */
public final class MongoDBOperators {

    public static final String AND = "$and";

    public static final String OR = "$or";

    public static final String GT = "$gt";

    public static final String GTE = "$gte";

    public static final String LT = "$lt";

    public static final String LTE = "$lte";

    public static final String NE = "$ne";

    public static final String IN = "$in";

    public static final String NIN = "$nin";

    public static final String NOT = "$not";

    public static final String ELEM_MATCH = "$elemMatch";

    public static final String TEXT = "$text";

    public static final String SEARCH = "$search";

    private MongoDBOperators() {
        // no instance allowed
    }

}
