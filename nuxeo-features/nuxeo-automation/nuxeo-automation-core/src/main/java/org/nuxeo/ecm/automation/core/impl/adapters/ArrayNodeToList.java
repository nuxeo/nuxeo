/*
 * Copyright (c) 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;

/**
 * Make it possible to directly JSON tree nodes parsed by the REST API (e.g.
 * parameters or input) directly to java datastructures.
 *
 * @author Olivier Grisel
 * @since 5.7
 */
public class ArrayNodeToList implements TypeAdapter {

    ObjectMapper mapper = new ObjectMapper();

    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt)
            throws TypeAdaptException {
        return mapper.convertValue(objectToAdapt, List.class);
    }

}
