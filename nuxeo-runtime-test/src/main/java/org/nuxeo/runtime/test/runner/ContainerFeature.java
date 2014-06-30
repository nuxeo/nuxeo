/*******************************************************************************
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *******************************************************************************/
package org.nuxeo.runtime.test.runner;

import java.util.Properties;

import org.nuxeo.runtime.jtajca.JtaActivator;

@Deploy({"org.nuxeo.runtime.jtajca", "org.nuxeo.runtime.datasource" })
public class ContainerFeature extends SimpleFeature {

    protected String autoactivationValue;

    @Override
    public void start(FeaturesRunner runner) {
        autoactivationValue = System.getProperty(JtaActivator.AUTO_ACTIVATION);
        System.setProperty(JtaActivator.AUTO_ACTIVATION, "true");
    }

    @Override
    public void stop(FeaturesRunner runner) {
        Properties props = System.getProperties();
        if (autoactivationValue != null) {
            props.put(JtaActivator.AUTO_ACTIVATION, autoactivationValue);
        } else {
            props.remove(JtaActivator.AUTO_ACTIVATION);
        }
    }
}