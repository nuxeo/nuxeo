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
package org.nuxeo.connect.tools.report.apidoc;

import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.connect.tools.report.ReportWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Distribution snapshot report.
 *
 * @see DistributionSnapshot
 *
 * @since 8.3
 */
public class APIDocReport implements ReportWriter {

    @Override
    public void write(OutputStream sink) throws IOException {
        DistributionSnapshot.jsonWriter().with(SerializationFeature.WRAP_ROOT_VALUE).writeValue(sink, RuntimeSnapshot.build());
    }

}
