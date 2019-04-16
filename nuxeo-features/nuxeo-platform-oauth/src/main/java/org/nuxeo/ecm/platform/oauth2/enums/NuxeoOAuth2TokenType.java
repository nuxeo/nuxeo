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

package org.nuxeo.ecm.platform.oauth2.enums;

import static org.nuxeo.ecm.platform.oauth2.Constants.TOKEN_SERVICE;
import static org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token.KEY_SERVICE_NAME;

import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Predicates;

/**
 * OAuth2 token type can be:
 * <ul>
 * <li>Provided by Nuxeo: {@link #AS_PROVIDER}</li>
 * <li>Consumed by Nuxeo: {@link #AS_CLIENT}</li>
 * </ul>
 *
 * @since 11.1
 */
public enum NuxeoOAuth2TokenType {

    AS_PROVIDER("asProvider", Predicates.eq(KEY_SERVICE_NAME, TOKEN_SERVICE)),

    AS_CLIENT("asClient", Predicates.noteq(KEY_SERVICE_NAME, TOKEN_SERVICE));

    protected final String value;

    protected final Predicate predicate;

    NuxeoOAuth2TokenType(String value, Predicate predicate) {
        this.value = value;
        this.predicate = predicate;
    }

    public String getValue() {
        return value;
    }

    public Predicate getPredicate() {
        return predicate;
    }

}
