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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.elasticsearch.common.xcontent.DeprecationHandler.THROW_UNSUPPORTED_OPERATION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.GeoUtils;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;

/**
 * Abstract implementation of {@link ESHintQueryBuilder} that manages the geo queries.
 *
 * @since 11.1
 */
public abstract class AbstractGeoESHintQueryBuilder implements ESHintQueryBuilder {

    protected String[] validate(Object value, int arrayLength, String message) {
        if (!(value instanceof Object[])) {
            throw new NuxeoException(String.format("Expected an array, found %s", value.getClass()), SC_BAD_REQUEST);
        }

        Object[] values = (Object[]) value;
        if (values.length != arrayLength) {
            throw new NuxeoException(String.format("Hints: %s requires %s parameters: %s", getClass().getSimpleName(),
                    arrayLength, message), SC_BAD_REQUEST);
        }

        return Arrays.asList(values).toArray(new String[0]);
    }

    protected GeoPoint parseGeoPointString(String value) {
        try {
            XContentBuilder content = JsonXContent.contentBuilder();
            content.value(value);
            content.flush();
            content.close();
            try (XContentParser parser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY,
                    THROW_UNSUPPORTED_OPERATION, ((ByteArrayOutputStream) content.getOutputStream()).toByteArray())) {
                parser.nextToken();
                return GeoUtils.parseGeoPoint(parser);
            }
        } catch (IOException | ElasticsearchParseException e) {
            throw new NuxeoException(String.format("Invalid value for Geo-point: %s", value), e, SC_BAD_REQUEST);
        }
    }
}
