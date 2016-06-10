/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Listener performing operations when dealing with an error. Order of methods called:
 * <ul>
 * <li>1. startHandling</li>
 * <li>2. beforeSetErrorPageAttribute</li>
 * <li>3. beforeForwardToErrorPage</li>
 * <li>4. afterDispatch</li>
 * </ul>
 *
 * @author arussel
 */
public interface ExceptionHandlingListener {

    /**
     * Error has happened, things to do before error is dealt with.
     */
    void startHandling(Throwable t, HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException;

    void beforeSetErrorPageAttribute(Throwable t, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException;

    void beforeForwardToErrorPage(Throwable t, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException;

    void afterDispatch(Throwable t, HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException;

    void responseComplete();

}
