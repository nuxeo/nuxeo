/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import org.nuxeo.ecm.core.schema.types.Constraint;

/**
 * String length constraint.
 * <p>
 * The length constraint is not strict (i.e. >= and <=).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StringLengthConstraint implements Constraint {

    /**
     *
     */
    private static final long serialVersionUID = 3630463971175189087L;
    private final int min;
    private final int max;

    public StringLengthConstraint(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean validate(Object object) {
        int len = object.toString().length();
        return len <= max && len >= min;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

}
