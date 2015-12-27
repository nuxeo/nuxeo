/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform;

import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.3
 */
public class FakeHandler implements Handler<MessageContext> {

    private static final Log log = LogFactory.getLog(FakeHandler.class);

    @Override
    public boolean handleMessage(MessageContext context) {
        log.info(this.getClass().getName() + " handleMessage");
        return false;
    }

    @Override
    public boolean handleFault(MessageContext context) {
        log.info(this.getClass().getName() + " handleFault");
        return false;
    }

    @Override
    public void close(MessageContext context) {
        log.info(this.getClass().getName() + " close");
    }
}
