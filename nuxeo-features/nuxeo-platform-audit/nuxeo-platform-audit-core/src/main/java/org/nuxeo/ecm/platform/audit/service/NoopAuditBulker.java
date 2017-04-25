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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBulkerDescriptor;

/**
 *
 *
 * @since 8.3
 */
public class NoopAuditBulker implements AuditBulker{

    final AuditBackend backend;

    NoopAuditBulker(AuditBackend backend, AuditBulkerDescriptor config) {
        this.backend = backend;
    }

    @Override
    public void onApplicationStarted() {

    }

    @Override
    public void onStandby() {

    }

    @Override
    public void offer(LogEntry entry) {
        backend.addLogEntries(Collections.singletonList(entry));
    }

    @Override
    public boolean await(long delay, TimeUnit unit) throws InterruptedException {
        return true;
    }

}
