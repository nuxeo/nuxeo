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
package org.nuxeo.ecm.platform.audit.service;

import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 *
 *
 * @since 8.3
 */
public interface AuditBulker {

    void onApplicationStarted();

    /**
     * @since 9.2 with default backward compatibility by delegating to deprecated API {@link #onShutdown()}
     */
    default void onStandby() {
        onShutdown();
    }

    void offer(LogEntry entry);

    boolean await(long delay, TimeUnit unit) throws InterruptedException;

    /**
     *
     *
     *
     * @deprecated since 9.2, replaced with {@link #onStandby()}
     */
    @Deprecated
    default void onShutdown() {
        throw new AssertionError("deprecated API, should not be invoked");
    }

}