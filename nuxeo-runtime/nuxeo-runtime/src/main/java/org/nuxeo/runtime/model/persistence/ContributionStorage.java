/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ContributionStorage {

    Contribution addContribution(Contribution contribution) throws Exception;

    Contribution getContribution(String name) throws Exception;

    List<Contribution> getContributions() throws Exception;

    boolean removeContribution(Contribution name) throws Exception;

    Contribution updateContribution(Contribution contribution) throws Exception;

}
