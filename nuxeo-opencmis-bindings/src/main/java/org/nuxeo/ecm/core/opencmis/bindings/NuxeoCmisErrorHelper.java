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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to deal with HTTP errors.
 *
 * @since 7.1
 */
public class NuxeoCmisErrorHelper {

    public static final String EXTRACTOR_CLASS_PROP = "org.nuxeo.cmis.errorextractor";

    public static ErrorExtractor errorExtractor;

    /**
     * Interface for a helper able to extract the error from an exception.
     *
     * @since 7.4
     */
    public interface ErrorExtractor {

        /**
         * Extracts the error from the exception.
         *
         * @param ex the exception
         * @return the error info
         */
        ErrorInfo extractError(Exception ex);
    }

    /**
     * Info about an error to return to client code.
     *
     * @since 7.1
     */
    public static class ErrorInfo {
        public int statusCode;

        public String exceptionName;

        public String message;

        public ErrorInfo(int statusCode, String exceptionName, String message) {
            this.statusCode = statusCode;
            this.exceptionName = exceptionName;
            this.message = message;
        }
    }

    public static ErrorInfo extractError(Exception ex) {
        if (errorExtractor == null) {
            String className = Framework.getProperty(EXTRACTOR_CLASS_PROP);
            if (StringUtils.isBlank(className)) {
                className = DefaultErrorExtractor.class.getName();
            }
            try {
                errorExtractor = (ErrorExtractor) Class.forName(className).newInstance();
            } catch (ReflectiveOperationException | ClassCastException e) {
                throw new RuntimeException("Cannot instantiate " + className, e);
            }
        }
        return errorExtractor.extractError(ex);
    }

}
