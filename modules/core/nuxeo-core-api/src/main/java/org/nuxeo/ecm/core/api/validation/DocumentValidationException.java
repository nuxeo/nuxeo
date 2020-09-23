/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Exception thrown when some process failed due to {@link ValidationViolation}.
 *
 * @since 7.1
 */
public class DocumentValidationException extends NuxeoException {

    protected static final String MESSAGE_SINGLE = "Constraint violation thrown: '%s'";

    protected static final String MESSAGE = "%s constraint violation(s) thrown. First one is: '%s', call "
            + DocumentValidationException.class.getSimpleName() + ".getViolations() to get the others";

    private static final long serialVersionUID = 1L;

    private DocumentValidationReport report;

    public DocumentValidationException(DocumentValidationReport report) {
        super(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        this.report = report;
    }

    public DocumentValidationException(String message) {
        super(message, HttpStatus.SC_UNPROCESSABLE_ENTITY);
        List<ValidationViolation> violations = new ArrayList<>();
        violations.add(new GlobalViolation(message));
        this.report = new DocumentValidationReport(violations);
    }

    public DocumentValidationReport getReport() {
        return report;
    }

    @Override
    public String getMessage() {
        if (report.hasError()) {
            int num = report.numberOfErrors();
            String violationMessage = report.asList().get(0).getMessage(null);
            if (num > 1) {
                return String.format(MESSAGE, report.numberOfErrors(), violationMessage);
            } else {
                return String.format(MESSAGE_SINGLE, violationMessage);
            }
        } else {
            return super.getMessage();
        }
    }

}
