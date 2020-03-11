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

package org.nuxeo.ecm.platform.oauth2.tokens;

import static java.util.Objects.requireNonNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token.KEY_NUXEO_LOGIN;
import static org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token.KEY_SERVICE_NAME;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.enums.NuxeoOAuth2TokenType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The implementation that manages the oauth2 tokens in Nuxeo.
 *
 * @since 11.1
 */
public class OAuth2TokenServiceImpl extends DefaultComponent implements OAuth2TokenService {

    public static final String TOKEN_DIR = "oauth2Tokens";

    @Override
    public List<NuxeoOAuth2Token> getTokens(NuxeoPrincipal principal) {
        return findTokens(getQueryBuilder(null, null), principal);
    }

    @Override
    public List<NuxeoOAuth2Token> getTokens(String nxuser) {
        requireNonNull(nxuser, "nxuser cannot be null");
        return findTokens(getQueryBuilder(nxuser, null));
    }

    @Override
    public List<NuxeoOAuth2Token> getTokens(NuxeoOAuth2TokenType type, NuxeoPrincipal principal) {
        requireNonNull(type, "oAuth2TokenType cannot be null");
        return findTokens(getQueryBuilder(null, type), principal);
    }

    @Override
    public List<NuxeoOAuth2Token> getTokens(String nxuser, NuxeoOAuth2TokenType type) {
        requireNonNull(nxuser, "nxuser cannot be null");
        requireNonNull(type, "oAuth2TokenType cannot be null");

        return findTokens(getQueryBuilder(nxuser, type));
    }

    @Override
    public List<NuxeoOAuth2Token> search(String query, NuxeoPrincipal principal) {
        return findTokens(getQueryBuilder(query), principal);
    }

    protected List<NuxeoOAuth2Token> findTokens(QueryBuilder queryBuilder) {
        return findTokens(queryBuilder, null);
    }

    protected List<NuxeoOAuth2Token> findTokens(QueryBuilder queryBuilder, NuxeoPrincipal principal) {
        if (principal != null) {
            checkPermission(principal);
        }
        return Framework.doPrivileged(() -> {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            try (Session session = ds.open(TOKEN_DIR)) {
                List<DocumentModel> documents = session.query(queryBuilder, false);
                return documents.stream().map(NuxeoOAuth2Token::new).collect(Collectors.toList());
            }
        });
    }

    protected void checkPermission(NuxeoPrincipal principal) {
        if (!principal.isAdministrator()) {
            throw new NuxeoException("You do not have permissions to perform this operation.", SC_FORBIDDEN);
        }
    }

    protected QueryBuilder getQueryBuilder(String query) {
        if (StringUtils.isEmpty(query)) {
            throw new NuxeoException("query is required", SC_BAD_REQUEST);
        }

        String match = String.format("%%%s%%", query);
        return new QueryBuilder().predicate(
                Predicates.or(Predicates.like(KEY_NUXEO_LOGIN, match), Predicates.like(KEY_SERVICE_NAME, match)));

    }

    protected QueryBuilder getQueryBuilder(String nxuser, NuxeoOAuth2TokenType type) {
        QueryBuilder queryBuilder = new QueryBuilder();

        if (nxuser != null) {
            queryBuilder.predicate(Predicates.eq(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, nxuser));
        }
        if (type != null) {
            queryBuilder.predicate(type.getPredicate());
        }
        return queryBuilder;
    }
}
