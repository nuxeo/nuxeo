/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.mail;

import org.apache.commons.exec.LogOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link java.io.OutputStream} used within a {@link javax.mail.Session} to print debug logs.
 * 
 * @since 11.1
 */
public class MailLogOutputStream extends LogOutputStream {

    private static final Logger log = LogManager.getLogger("javax.mail.Session");

    @Override
    protected void processLine(String line, int logLevel) {
        log.debug("{}", () -> line.replace("DEBUG: ", ""));
    }
}
