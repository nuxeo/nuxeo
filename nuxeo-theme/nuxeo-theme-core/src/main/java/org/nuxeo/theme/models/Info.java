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

package org.nuxeo.theme.models;

import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.engines.EngineType;

public interface Info {

    Info createCopy();

    Model getModel();

    void setModel(Model model);

    String getMarkup();

    void setMarkup(String markup);

    EngineType getEngine();

    String getViewMode();

    Element getElement();

    boolean isDirty();

    void setDirty(boolean dirty);

}
