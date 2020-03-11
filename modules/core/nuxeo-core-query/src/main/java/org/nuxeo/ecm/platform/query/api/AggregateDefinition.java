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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.platform.query.api;

import java.util.List;
import java.util.Map;

/**
 * @since 6.0
 */
public interface AggregateDefinition {

    String getId();

    void setId(String id);

    String getType();

    void setType(String type);

    void setProperty(String name, String value);

    Map<String, String> getProperties();

    List<AggregateRangeDefinition> getRanges();

    void setRanges(List<AggregateRangeDefinition> ranges);

    List<AggregateRangeDateDefinition> getDateRanges();

    void setDateRanges(List<AggregateRangeDateDefinition> ranges);

    /**
     * Get the document aggregator field
     */
    String getDocumentField();

    void setDocumentField(String parameter);

    /**
     * Get the ref of the search input, the type of the field must be nxs:stringList
     */
    PredicateFieldDefinition getSearchField();

    void setSearchField(PredicateFieldDefinition field);

    AggregateDefinition clone();

    /**
     * @return a map associating the key of the date range to its position in the definition.
     */
    Map<String, Integer> getAggregateDateRangeDefinitionOrderMap();

    /**
     * @return a map associating the key of the range to its position in the definition.
     */
    Map<String, Integer> getAggregateRangeDefinitionOrderMap();

}
