/*
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
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.connect.tools.report.management;

import java.io.IOException;

import javax.json.JsonObject;
import javax.management.JMException;

import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 *
 * @since 8.3
 */
public class MXComponent extends DefaultComponent {

    static MXComponent instance;

    interface Invoker {

        JsonObject list()
                throws IOException, JMException;

        JsonObject search(String pattern)
                throws IOException, JMException;

        JsonObject read(String pattern)
                throws IOException, JMException;

        JsonObject exec(String pattern, String operation, Object... arguments)
                throws IOException, JMException;

        void destroy();

    }

    Invoker invoker;

    @Override
    public void applicationStarted(ComponentContext context) {
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
                    return;
                }
                Framework.removeListener(this);
                try {
                    invoker.destroy();
                } finally {
                    instance = null;
                    invoker = null;
                }
            }

        });
        invoker = new JolokiaInvoker();
        instance = this;
    }

}
