/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.formats.styles;

import java.util.Properties;
import java.util.Set;

import org.nuxeo.theme.formats.Format;

public interface Style extends Format {

    Properties getPropertiesFor(String viewName, String path);

    void setPropertiesFor(String viewName, String path, Properties properties);

    void clearPropertiesFor(String viewName);

    void clearPropertiesFor(String viewName, String path);

    Set<String> getPathsForView(String viewName);

    Set<String> getSelectorViewNames();

    String getSelectorDescription(String path, String viewName);

    void setSelectorDescription(String path, String viewName, String description);

    Properties getAllProperties();

    void setCollection(String collectionName);

    String getCollection();

}
