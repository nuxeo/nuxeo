/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.management.metrics;

import org.javasimon.SimonManager;
import org.javasimon.jmx.JmxRegisterCallback;
import org.javasimon.jmx.SimonSuperMXBean;

public class MetricRegisteringCallback extends JmxRegisterCallback {

    @Override
    public void initialize() {
        for (String name : SimonManager.simonNames()) {
            this.simonCreated(SimonManager.getSimon(name));
        }
    }


    @Override
    protected String constructObjectName(SimonSuperMXBean simonMxBean) {
        return String.format("org.nuxeo:metric=%s,management=metric", simonMxBean.getName());
    }
}
