/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
public class FakeHandler implements Handler {

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
