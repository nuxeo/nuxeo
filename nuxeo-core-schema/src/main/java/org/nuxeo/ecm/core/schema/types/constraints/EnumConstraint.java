/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.schema.types.Constraint;

/**
 * Constraint based on String enumeration
 * 
 * NB : for now, the validation is not done.
 * 
 * @since 5.7
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class EnumConstraint implements Constraint {

    private static final long serialVersionUID = 1L;

    protected final List<String> possibleValues;

    public EnumConstraint(List<String> possibleValues) {
        this.possibleValues = new ArrayList<String>();
        this.possibleValues.addAll(possibleValues);
    }

    @Override
    public boolean validate(Object object) {
        // for now : we don't validate
        // validation should not be done at low level otherwise
        // we can not return an understandable error to the user
        return true;
    }

    public List<String> getPossibleValues() {
        return Collections.unmodifiableList(possibleValues);
    }

}
