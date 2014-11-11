/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        return getQueryTransformer();
    }

    /**
     * Legacy method for compatibility, use
     * {@link #getQueryTransformer(String)} instead
     */
    @Deprecated
    public Transformer getQueryTransformer() {
        // implement this if isExpressibleInQuery is true
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        return isExpressibleInQuery();
    }

    /**
     * Legacy method for compatibility, use
     * {@link #isExpressibleInQuery(String)} instead
     */
    @Deprecated
    public boolean isExpressibleInQuery() {
        // by default, we don't know, so no
        return false;
    }

}
