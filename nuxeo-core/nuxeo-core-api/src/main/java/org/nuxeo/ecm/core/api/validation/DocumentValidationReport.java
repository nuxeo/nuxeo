/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.validation;

import java.util.Collections;
import java.util.List;

/**
 * A managed set of {@link ConstraintViolation}.
 *
 * @since 7.1
 */
public class DocumentValidationReport {

    protected List<ConstraintViolation> violations;

    public DocumentValidationReport(List<ConstraintViolation> violations) {
        super();
        this.violations = violations;
    }

    public boolean hasError() {
        return !violations.isEmpty();
    }

    public int numberOfErrors() {
        return violations.size();
    }

    public List<ConstraintViolation> asList() {
        return Collections.unmodifiableList(violations);
    }

}
