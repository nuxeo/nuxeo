/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca;

import java.util.List;

import javax.management.MXBean;

/**
 * @author matic
 */
@MXBean
public interface TransactionMonitor extends Monitor {

    String NAME = Defaults.instance.name(TransactionMonitor.class);

    long getActiveCount();

    long getTotalCommits();

    long getTotalRollbacks();

    List<TransactionStatistics> getActiveStatistics();

    TransactionStatistics getLastCommittedStatistics();

    TransactionStatistics getLastRollbackedStatistics();

    boolean getEnabled();

    boolean toggle();

}
