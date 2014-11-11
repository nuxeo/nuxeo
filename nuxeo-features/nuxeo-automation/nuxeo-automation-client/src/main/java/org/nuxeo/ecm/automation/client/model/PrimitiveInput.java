/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.model;



/**
 * @author matic
 *
 */
public class PrimitiveInput<T> implements OperationInput {

    private static final long serialVersionUID = -6717232462627061723L;

    public PrimitiveInput(T value) {
        this.value = value;
        this.type= value.getClass().getSimpleName().toLowerCase();
    }

    protected final T value;

    protected final String type;

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public String getInputType() {
        return type;
    }

    @Override
    public String getInputRef() {
        return String.format("%s:%s", type, value.toString());
    }

}
