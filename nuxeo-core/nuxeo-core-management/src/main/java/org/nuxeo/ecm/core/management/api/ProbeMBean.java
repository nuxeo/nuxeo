/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.management.api;

import java.util.Date;

import org.nuxeo.runtime.management.api.ProbeStatus;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public interface ProbeMBean {

    boolean isEnabled();

    void enable();

    void disable();

    boolean isInError();

    long getRunnedCount();

    Date getLastRunnedDate();

    long getLastDuration();

    long getSucceedCount();

    Date getLastSucceedDate();

    long getFailedCount();

    Date getLastFailedDate();

    ProbeStatus getLastFailureStatus();

}
