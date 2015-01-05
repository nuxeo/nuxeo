/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     jcarsique
 */
package org.nuxeo.wizard.download;

public enum PendingDownloadStatus {
    CORRUPTED(-3), MISSING(-2), ABORTED(-1), PENDING(0), INPROGRESS(1), COMPLETED(2), VERIFICATION(3), VERIFIED(4);

    private final int value;

    private PendingDownloadStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
