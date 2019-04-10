/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;

import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisFilterNotValidException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNameConstraintViolationException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisStorageException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisStreamNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUpdateConflictException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisVersioningException;
import org.apache.commons.lang3.math.NumberUtils;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisErrorHelper.ErrorExtractor;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisErrorHelper.ErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to deal with HTTP errors.
 *
 * @since 7.1
 */
public class DefaultErrorExtractor implements ErrorExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultErrorExtractor.class);

    // see CmisAtomPubServlet.printError
    // see CmisBrowserBindingServlet.ErrorServiceCall.printError
    @Override
    public ErrorInfo extractError(Exception ex) {
        int statusCode = SC_INTERNAL_SERVER_ERROR; // 500
        String exceptionName = "runtime";

        if (ex instanceof CmisRuntimeException) {
            Throwable cause = ex.getCause();
            if (cause instanceof RecoverableClientException) {
                // don't log something harsh in that case
                statusCode = getHttpStatus((RecoverableClientException) cause);
            } else {
                LOG.error(ex.getMessage(), ex);
            }
        } else if (ex instanceof CmisStorageException) {
            LOG.error(ex.getMessage(), ex);
            statusCode = getErrorCode((CmisStorageException) ex);
            exceptionName = ((CmisStorageException) ex).getExceptionName();
        } else if (ex instanceof CmisBaseException) {
            statusCode = getErrorCode((CmisBaseException) ex);
            exceptionName = ((CmisBaseException) ex).getExceptionName();
        } else if (ex instanceof IOException) {
            LOG.warn(ex.getMessage(), ex);
        } else {
            LOG.error(ex.getMessage(), ex);
        }

        String message = ex.getMessage();
        if (!(ex instanceof CmisBaseException)) {
            message = "An error occurred!";
        }

        return new ErrorInfo(statusCode, exceptionName, message);
    }

    /*
     * A bit of a hack, we need a way to find the HTTP status from the exception. We use the last parameter of the
     * localized message for that.
     */
    public int getHttpStatus(RecoverableClientException ex) {
        String[] params = ex.geLocalizedMessageParams(); // urgh, typo
        int len = params == null ? 0 : params.length;
        String lastParam;
        if (len > 0 && NumberUtils.isDigits(lastParam = params[len - 1])) {
            try {
                return Integer.parseInt(lastParam);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return SC_INTERNAL_SERVER_ERROR; // 500
    }

    // see CmisAtomPubServlet.getErrorCode
    // see CmisBrowserBindingServlet.ErrorServiceCall.getErrorCode
    public int getErrorCode(CmisBaseException ex) {
        if (ex instanceof CmisConstraintException) {
            return SC_CONFLICT; // 409
        } else if (ex instanceof CmisContentAlreadyExistsException) {
            return SC_CONFLICT; // 409
        } else if (ex instanceof CmisFilterNotValidException) {
            return SC_BAD_REQUEST; // 400
        } else if (ex instanceof CmisInvalidArgumentException) {
            return SC_BAD_REQUEST; // 400
        } else if (ex instanceof CmisNameConstraintViolationException) {
            return SC_CONFLICT; // 409
        } else if (ex instanceof CmisNotSupportedException) {
            return SC_METHOD_NOT_ALLOWED; // 405
        } else if (ex instanceof CmisObjectNotFoundException) {
            return SC_NOT_FOUND; // 404
        } else if (ex instanceof CmisPermissionDeniedException) {
            return SC_FORBIDDEN; // 403
        } else if (ex instanceof CmisStorageException) {
            return SC_INTERNAL_SERVER_ERROR; // 500
        } else if (ex instanceof CmisStreamNotSupportedException) {
            return SC_FORBIDDEN; // 403
        } else if (ex instanceof CmisUpdateConflictException) {
            return SC_CONFLICT; // 409
        } else if (ex instanceof CmisVersioningException) {
            return SC_CONFLICT; // 409
        }
        return SC_INTERNAL_SERVER_ERROR; // 500
    }

}
