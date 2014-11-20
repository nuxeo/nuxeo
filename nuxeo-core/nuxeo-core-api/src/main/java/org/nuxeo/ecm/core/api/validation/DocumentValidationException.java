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

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.schema.types.constraints.ConstraintViolation;

/**
 * Exception thrown when some process failed due to {@link ConstraintViolation}.
 *
 * @since 7.1
 */
public class DocumentValidationException extends ClientException {

    private static final String MESSAGE = "%s constraint violation(s) where thrown. 1st is : %s (call "
            + DocumentValidationException.class.getSimpleName() + ".getViolations() to get the others)";

    private static final long serialVersionUID = 1L;

    private List<ConstraintViolation> violations;

    public DocumentValidationException(List<ConstraintViolation> violations) {
        super();
        this.violations = violations;
    }

    public List<ConstraintViolation> getViolations() {
        return violations;
    }

    @Override
    public String getMessage() {
        if (violations.size() > 0) {
            String violationMessage = violations.get(0).getMessage(null);
            String message = String.format(MESSAGE, violations.size(), violationMessage);
            return message;
        } else {
            return super.getMessage();
        }
    }

}
