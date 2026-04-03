/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.schema.test;

/**
 * @since 2025.0
 */
public final class CommonDocumentConstants {

    // Document Type & Facet

    public static final String COMMON_DOC_TYPE = "CommonDocument";

    public static final String COMMON_ADDITIONAL_COMPLEX_FACET = "CommonAdditionalComplex";

    // Schema

    public static final String COMMON_SCALAR_SCHEMA = "testCommonScalar";

    public static final String COMMON_COMPLEX_SCHEMA = "testCommonComplex";

    /** @since 2025.18 */
    public static final String COMMON_UNPREFIXED_SCHEMA = "testCommonUnprefixed";

    // Schema tcs / testCommonScalar Property

    public static final String COMMON_BOOLEAN_PROP = "tcs:boolean";

    public static final String COMMON_DATE_PROP = "tcs:date";

    public static final String COMMON_DOUBLE_PROP = "tcs:double";

    public static final String COMMON_FLOAT_PROP = "tcs:float";

    public static final String COMMON_INTEGER_PROP = "tcs:integer";

    public static final String COMMON_LONG_PROP = "tcs:long";

    public static final String COMMON_STRING_PROP = "tcs:string";

    public static final String COMMON_DEFAULT_LONG_PROP = "tcs:defaultLong";

    public static final String COMMON_DEFAULT_STRING_PROP = "tcs:defaultString";

    public static final String COMMON_DOUBLES_PROP = "tcs:doubles";

    public static final String COMMON_INTEGERS_PROP = "tcs:integers";

    public static final String COMMON_LONGS_PROP = "tcs:longs";

    public static final String COMMON_STRINGS_PROP = "tcs:strings";

    public static final String COMMON_ARRAY_DOUBLE_PROP = "tcs:arrayDouble";

    public static final String COMMON_ARRAY_INTEGER_PROP = "tcs:arrayInteger";

    public static final String COMMON_ARRAY_LONG_PROP = "tcs:arrayLong";

    public static final String COMMON_ARRAY_STRING_PROP = "tcs:arrayString";

    // Schema tcc / testCommonComplex Property

    public static final String COMMON_COMPLEX_PROP = "tcc:complex";

    public static final String COMMON_COMPLEX_BOOLEAN_PROP = "tcc:complex/boolean";

    public static final String COMMON_COMPLEX_DATE_PROP = "tcc:complex/date";

    public static final String COMMON_COMPLEX_DOUBLE_PROP = "tcc:complex/double";

    public static final String COMMON_COMPLEX_INTEGER_PROP = "tcc:complex/integer";

    public static final String COMMON_COMPLEX_LONG_PROP = "tcc:complex/long";

    public static final String COMMON_COMPLEX_STRING_PROP = "tcc:complex/string";

    public static final String COMMON_COMPLEX_DEFAULT_STRING_PROP = "tcc:complex/defaultString";

    public static final String COMMON_COMPLEX_DOUBLES_PROP = "tcc:complex/doubles";

    public static final String COMMON_COMPLEX_INTEGERS_PROP = "tcc:complex/integers";

    public static final String COMMON_COMPLEX_LONGS_PROP = "tcc:complex/longs";

    public static final String COMMON_COMPLEX_STRINGS_PROP = "tcc:complex/strings";

    public static final String COMMON_COMPLEXES_PROP = "tcc:complexes";

    public static final String COMMON_COMPLEX2_PROP = "tcc:complex2";

    // Schema testCommonUnprefixed Property

    /** @since 2025.18 */
    public static final String COMMON_UNPREFIXED_STRING = "testCommonUnprefixed:common_unprefixed_string";

    /** @since 2025.18 */
    public static final String COMMON_UNPREFIXED_STRING_SHORT = "common_unprefixed_string";

    private CommonDocumentConstants() {
        // constants class
    }
}
