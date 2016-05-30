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
package org.nuxeo.connect.tools.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.JsonObject;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.runtime.management", "org.nuxeo.connect.tools", "org.nuxeo.apidoc.core" })
public class ReportFeature extends SimpleFeature {

    JsonObject snapshot(String name) throws IOException {
        for (ReportContribution contrib : ReportComponent.instance.configuration) {
            if (contrib.name.equals(name)) {
                return snapshot(contrib.instance);
            }
        }
        throw new AssertionError("Cannot find report of name " + name);
    }

    JsonObject snapshot(Report report) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            return report.snapshot();
        }
    }
}
