/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.io.download.DownloadHelper;

public class ExceptionHelper {

    private ExceptionHelper() {
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;
        if (t instanceof ServletException) {
            cause = ((ServletException) t).getRootCause();
        } else if (t instanceof Exception) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

    protected static final List<String> ERROR_MESSAGES = Arrays.asList("java.lang.SecurityException",
            DocumentSecurityException.class.getName(), SecurityException.class.getName());

    public static boolean isSecurityError(Throwable t) {
        if (t == null) {
            return false;
        } else if (t instanceof DocumentSecurityException || t.getCause() instanceof DocumentSecurityException
                || t.getCause() instanceof SecurityException) {
            return true;
        } else if (t.getMessage() != null) {
            String message = t.getMessage();
            for (String errorMessage : ERROR_MESSAGES) {
                if (message.contains(errorMessage)) {
                    return true;
                }
            }
        }
        return false;
    }

}
