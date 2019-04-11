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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.log;

/**
 * @author Thierry Delprat
 */
public interface ImporterLogger {

    void info(String message);

    void warn(String message);

    void debug(String message);

    void debug(String message, Throwable t);

    void error(String message);

    void error(String message, Throwable t);

    String getLoggerBuffer(String sep);

    String getLoggerBuffer();

    boolean isBufferActive();

    void setBufferActive(boolean active);

}
