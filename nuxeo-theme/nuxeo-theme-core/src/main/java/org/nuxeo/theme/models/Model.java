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

import java.util.List;

public interface Model {

    String getModelTypeName();

    ModelType getModelType();

    Model addItem(Model model) throws ModelException;

    Model insertItem(int order, Model model) throws ModelException;

    List<Model> getItems();

    boolean hasItems();
}
