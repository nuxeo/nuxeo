/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.jms;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface MessageFactory extends Serializable {

    public static MessageFactory DEFAULT = new MessageFactory() {
        private static final long serialVersionUID = -5609402306633995881L;
        public Message createMessage(Session session, Object object) throws JMSException {
            if (object instanceof Serializable) {
                return session.createObjectMessage((Serializable)object);
            } else {
                throw new JMSException("Cannot create an object message: the input object is not serializable");
            }
        }
    };

    Message createMessage(Session session, Object object) throws JMSException;

}
