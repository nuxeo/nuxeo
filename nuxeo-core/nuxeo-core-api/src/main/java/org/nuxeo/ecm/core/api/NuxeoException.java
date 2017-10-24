/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * The most generic exception thrown by the Nuxeo.
 * <p>
 * It can be used to provide enriched information on the exception catch path, without re-wrapping:
 *
 * <pre>
 * try {
 *     doSomething(id);
 * } catch (NuxeoException e) {
 *     e.addInfo("Failed to do something with document id: " + id);
 *     throw e;
 * }
 * </pre>
 */
public class NuxeoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private LinkedList<String> infos;

    protected int statusCode = SC_INTERNAL_SERVER_ERROR;

    public NuxeoException() {
    }

    /**
     * @since 9.3
     */
    public NuxeoException(int statusCode) {
        this.statusCode = statusCode;
    }

    public NuxeoException(String message) {
        super(message);
    }

    /**
     * @since 9.3
     */
    public NuxeoException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public NuxeoException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @since 9.3
     */
    public NuxeoException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public NuxeoException(Throwable cause) {
        super(cause);
    }

    /**
     * @since 9.3
     */
    public NuxeoException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    /**
     * Adds information to this exception, to be returned with the message.
     *
     * @param info the information
     * @since 7.4
     */
    public void addInfo(String info) {
        if (infos == null) {
            infos = new LinkedList<>();
        }
        infos.addFirst(info);
    }

    /**
     * Gets the information added to this exception.
     * <p>
     * The list is returned in the reverse order than that of the calls to {@link #addInfo}, i.e., the last added
     * information is first in the list.
     *
     * @return the information list
     * @since 7.4
     */
    public List<String> getInfos() {
        return infos == null ? Collections.emptyList() : infos;
    }

    /**
     * Gets the original message passed to the constructor, without additional information added.
     *
     * @since 7.4
     */
    public String getOriginalMessage() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        String message = getOriginalMessage();
        if (infos == null) {
            return message;
        } else {
            StringBuilder sb = new StringBuilder();
            for (String info : infos) {
                sb.append(info);
                sb.append(", ");
            }
            sb.append(message);
            return sb.toString();
        }
    }

    /**
     * Gets the HTTP status code mapped to this exception.
     *
     * @since 9.3
     */
    public int getStatusCode() {
        return statusCode;
    }

}
