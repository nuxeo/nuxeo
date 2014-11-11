/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.management.api;

import java.util.Collection;

public interface ProbeManager extends ProbeRunnerMBean {

    Collection<ProbeInfo> getAllProbeInfos();

    Collection<ProbeInfo> getInSuccessProbeInfos();

    Collection<ProbeInfo> getInFailureProbeInfos();

    ProbeInfo runProbe(ProbeInfo probe);

    ProbeInfo runProbe(String name);

    ProbeInfo getProbeInfo(String name);

    ProbeInfo getProbeInfo(Class<? extends Probe> probeClass);

}
