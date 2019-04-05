/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.elasticsearch.hint;

import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;

/**
 * The implementation of {@link ESHintQueryBuilder} for the <strong>"geo_bounding_box"</strong> Elasticsearch hint
 * operator.
 *
 * @since 11.1
 */
public class GeoBoundingBoxESHintQueryBuilder extends AbstractGeoESHintQueryBuilder {

    /**
     * {@inheritDoc}
     * <p>
     * 
     * @return {@link org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder}
     */
    @Override
    public QueryBuilder make(EsHint hint, String fieldName, Object value) {
        String[] values = validate(value, 2, "bottomLeft and topRight point");
        GeoPoint bottomLeft = parseGeoPointString(values[0]);
        GeoPoint topRight = parseGeoPointString(values[1]);
        return QueryBuilders.geoBoundingBoxQuery(fieldName).setCornersOGC(bottomLeft, topRight);
    }
}
