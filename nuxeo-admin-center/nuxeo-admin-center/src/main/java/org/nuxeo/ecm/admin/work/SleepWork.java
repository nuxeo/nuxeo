/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.admin.work;

import org.nuxeo.ecm.core.work.AbstractWork;

/**
 * Simple dummy work class for tests.
 */
public class SleepWork extends AbstractWork {

    protected long durationMillis;

    public SleepWork(long durationMillis) {
        this.durationMillis = durationMillis;
        setStatus("Sleep work");
    }

    @Override
    public String getCategory() {
        return "test";
    }

    @Override
    public void work() throws InterruptedException {
        while (true) {
            long current = System.currentTimeMillis();
            if (current > startTime + durationMillis) {
                break;
            }
            float pc = 100F * (current - startTime) / durationMillis;
            if (pc > 100) {
                pc = 100F;
            }
            setProgress(new Progress(pc));

            Thread.sleep(10);
        }
    }

}
