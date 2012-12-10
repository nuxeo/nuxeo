/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.nuxeo.ecm.automation.core.util.PaginableRecordSet;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class PaginableRecordSetImpl extends
        ArrayList<Map<String, Serializable>> implements PaginableRecordSet {

    private static final long serialVersionUID = 1L;

    protected final PageProvider<Map<String, Serializable>> provider;

    public PaginableRecordSetImpl(
            PageProvider<Map<String, Serializable>> provider) {
        super(provider.getCurrentPage());
        this.provider = provider;
    }

    @Override
    public long getCurrentPageIndex() {
        return provider.getCurrentPageIndex();
    }

    @Override
    public long getPageSize() {
        return provider.getPageSize();
    }

    @Override
    public long getNumberOfPages() {
        return provider.getNumberOfPages();
    }

}
