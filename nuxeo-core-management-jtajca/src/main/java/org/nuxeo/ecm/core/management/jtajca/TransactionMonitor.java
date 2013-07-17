/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca;

import java.util.List;

import javax.management.MXBean;

/**
 * @author matic
 * 
 */
@MXBean
public interface TransactionMonitor {

    public static String NAME = Defaults.instance.name(TransactionMonitor.class);

    long getActiveCount();

    long getTotalCommits();

    long getTotalRollbacks();

    List<TransactionStatistics> getActiveStatistics();

    TransactionStatistics getLastCommittedStatistics();

    TransactionStatistics getLastRollbackedStatistics();

}
