/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.management.standby;

import javax.management.MXBean;

/**
 * Allow administrators to toggle runtime standby mode.
 *
 * @since 9.2
 */
@MXBean
public interface StandbyMXBean {

    public static final String NAME = "org.nuxeo:name=org.nuxeo.ecm.core.management.standby,type=service";

    void standby(int delayInSeconds) throws InterruptedException;

    void resume();

    boolean isStandby();

}