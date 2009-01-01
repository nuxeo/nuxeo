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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.types.TypeFamily;

public abstract class AbstractModel implements Model {

    private List<Model> items = new ArrayList<Model>();

    public abstract String getModelTypeName();

    public ModelType getModelType() {
        return (ModelType) Manager.getTypeRegistry().lookup(TypeFamily.MODEL,
                getModelTypeName());
    }

    public Model addItem(Model model) throws ModelException {
        ModelType modelType = getModelType();
        if (modelType == null) {
            throw new ModelException("Model type not found: "
                    + getModelTypeName());
        }
        if (!getModelType().getAllowedTypes().contains(model.getModelTypeName())) {
            throw new ModelException("Model type: " + model.getModelTypeName()
                    + " not allowed in: " + this.getModelTypeName());
        }
        items.add(model);
        return model;
    }

    public Model insertItem(int index, Model model) throws ModelException {
        if (!getModelType().getAllowedTypes().contains(model.getModelTypeName())) {
            throw new ModelException("Model type: " + model.getModelTypeName()
                    + " not allowed in: " + this.getModelTypeName());
        }
        items.add(index, model);
        return model;
    }

    public List<Model> getItems() {
        return items;
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }
}
