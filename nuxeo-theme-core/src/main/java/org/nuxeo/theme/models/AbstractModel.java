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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.nodes.AbstractNode;
import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.types.TypeFamily;

public abstract class AbstractModel extends AbstractNode implements Model {

    private static final Log log = LogFactory.getLog(AbstractModel.class);

    public abstract String getModelTypeName();

    public ModelType getModelType() {
        return (ModelType) Manager.getTypeRegistry().lookup(TypeFamily.MODEL,
                getModelTypeName());
    }

    public Model addItem(Model model) {
        if (!getModelType().getAllowedTypes().contains(model.getModelTypeName())) {
            log.error("Model type: " + model.getModelTypeName()
                    + " not allowed in: " + this.getModelTypeName());
            return null;
        }
        addChild((Node) model);
        return model;
    }

    public List<Model> getItems() {
        List<Model> items = new ArrayList<Model>();
        for (Node node : getChildren()) {
            items.add((Model) node);
        }
        return items;
    }

}
