/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;

/**
 * Abstract security policy
 *
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public abstract class AbstractSecurityPolicy implements SecurityPolicy {

    @Override
    public boolean isRestrictingPermission(String permission) {
        // by default, we don't know, so yes
        return true;
    }

    @Override
    public Transformer getQueryTransformer(String repositoryName) {
        // implement this if isExpressibleInQuery is true
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryTransformer getQueryTransformer(String repositoryName, String queryLanguage) {
        /*
         * By default in this abstract class: If we're expressible in NXQL and the query transformer is IDENTITY, then
         * express as QueryTransformer.IDENTITY in any language.
         */
        if (isExpressibleInQuery(repositoryName) && getQueryTransformer(repositoryName) == Transformer.IDENTITY) {
            return QueryTransformer.IDENTITY;
        }
        // else we don't know how to transform
        throw new UnsupportedOperationException(queryLanguage);
    }

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        // by default, we don't know, so no
        return false;
    }

    @Override
    public boolean isExpressibleInQuery(String repositoryName, String queryLanguage) {
        /*
         * By default in this abstract class: If we're expressible in NXQL and the query transformer is IDENTITY, then
         * we're expressible (as QueryTransformer.IDENTITY) in any language.
         */
        return isExpressibleInQuery(repositoryName) && getQueryTransformer(repositoryName) == Transformer.IDENTITY;
    }

}
