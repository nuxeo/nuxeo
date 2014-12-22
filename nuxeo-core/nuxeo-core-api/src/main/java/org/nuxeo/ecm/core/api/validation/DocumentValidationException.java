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

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Exception thrown when some process failed due to {@link ConstraintViolation}.
 *
 * @since 7.1
 */
public class DocumentValidationException extends ClientException {

    private static final String MESSAGE = "%s constraint violation(s) where thrown. 1st is : %s (call "
            + DocumentValidationException.class.getSimpleName() + ".getViolations() to get the others)";

    private static final long serialVersionUID = 1L;

    private DocumentValidationReport report;

    public DocumentValidationException(DocumentValidationReport report) {
        super();
        this.report = report;
    }

    public DocumentValidationReport getReport() {
        return report;
    }

    @Override
    public String getMessage() {
        if (report.hasError()) {
            String violationMessage = report.asList().get(0).getMessage(null);
            String message = String.format(MESSAGE, report.numberOfErrors(), violationMessage);
            return message;
        } else {
            return super.getMessage();
        }
    }

}
