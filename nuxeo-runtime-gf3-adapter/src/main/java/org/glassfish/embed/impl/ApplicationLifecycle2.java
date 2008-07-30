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

package org.glassfish.embed.impl;

import java.util.LinkedList;

import org.glassfish.api.ActionReport;
import org.glassfish.api.container.Sniffer;
import org.glassfish.internal.data.ContainerInfo;
import org.glassfish.internal.data.ContainerRegistry;
import org.jvnet.hk2.annotations.Inject;

import com.sun.enterprise.v3.deployment.DeploymentContextImpl;
import com.sun.enterprise.v3.server.ApplicationLifecycle;
import com.sun.enterprise.v3.server.ProgressTracker;

/**
 *
 * Due to a bug in current impl. we need to override it
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationLifecycle2 extends ApplicationLifecycle {

    // container registry not visible from super class
    @Inject protected ContainerRegistry creg;


    @Override
    protected LinkedList<ContainerInfo> setupContainerInfos(
            Iterable<Sniffer> sniffers, DeploymentContextImpl context,
            ActionReport report, ProgressTracker tracker) throws Exception {
        LinkedList<ContainerInfo> result = super.setupContainerInfos(sniffers, context, report, tracker);

        if (result != null && result.isEmpty()) {

            for (Sniffer sniffer : sniffers) {
                for (String containerName : sniffer.getContainersNames()) {
                    ContainerInfo<?, ?> containerInfo = creg.getContainer(containerName);
                    if (containerInfo != null) {
                        result.add(containerInfo);
                    }
                }
            }
        }
        return result;
    }

}
