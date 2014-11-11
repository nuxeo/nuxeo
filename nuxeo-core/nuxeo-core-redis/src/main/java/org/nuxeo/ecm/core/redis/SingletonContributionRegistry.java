/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

public class SingletonContributionRegistry<T> extends
        SimpleContributionRegistry<T> {

    protected T main;

    @Override
    public String getContributionId(T contrib) {
        return "main";
    }

    @Override
    public void contributionUpdated(String id, T contrib, T newOrigContrib) {
        main = contrib;
    }

    @Override
    public void contributionRemoved(String id, T origContrib) {
        main = null;
    }
}
