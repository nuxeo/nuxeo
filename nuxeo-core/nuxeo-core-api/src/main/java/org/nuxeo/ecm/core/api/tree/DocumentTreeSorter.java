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

import org.nuxeo.ecm.core.api.Sorter;

/**
 * Interface for document tree sorter.
 *
 * @author Anahide Tchertchian
 */
public interface DocumentTreeSorter extends Sorter {

    String getSortPropertyPath();

    void setSortPropertyPath(String sortPropertyPath);

}
