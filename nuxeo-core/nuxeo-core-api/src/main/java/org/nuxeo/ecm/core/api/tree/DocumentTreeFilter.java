/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     george
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.tree;

import java.util.List;

import org.nuxeo.ecm.core.api.Filter;

/**
 * Interface for document tree filter.
 *
 * @author Anahide Tchertchian
 */
public interface DocumentTreeFilter extends Filter {

    List<String> getExcludedFacets();

    void setExcludedFacets(List<String> excludedFacets);

    List<String> getIncludedFacets();

    void setIncludedFacets(List<String> includedFacets);

    List<String> getExcludedTypes();

    void setExcludedTypes(List<String> excludedTypes);

}
