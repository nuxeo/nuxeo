package org.nuxeo.ecm.platform.importer.queue.manager;/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bdelbosc
 */

import net.openhft.chronicle.wire.AbstractMarshallable;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * @since 7.1
 */
public class MessageHandler {

    interface MessageListener {
        void receive(SourceNodeMessage message);
    }

    static class SourceNodeMessage extends AbstractMarshallable {
        String text;

        public SourceNodeMessage(SourceNode node) {
            this.text = node.getName();
        }
    }

    class MessageProcessor implements MessageListener {

        SourceNodeMessage msg = null;

        public MessageProcessor() {

        }

        @Override
        public void receive(SourceNodeMessage message) {
            message.text = message.text;
        }

        public SourceNodeMessage getNode() {
            return msg;
        }

    }

}
