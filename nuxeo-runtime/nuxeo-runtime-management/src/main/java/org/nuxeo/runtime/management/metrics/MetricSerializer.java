/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.management.metrics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Calendar;

import org.javasimon.Sample;
import org.javasimon.SimonManager;

import com.thoughtworks.xstream.XStream;

public class MetricSerializer implements MetricSerializerMXBean {

    protected File file;

    protected ObjectOutputStream outputStream;

    protected int count;

    protected long lastUsage;

    public void toStream(Sample... samples) throws IOException {
        if (outputStream == null) {
            return;
        }
        for (Sample sample : samples) {
                outputStream.writeObject(sample);
        }
        count += 1;
        lastUsage = Calendar.getInstance().getTimeInMillis();
    }


    @Override
    public String getOutputLocation() {
        if (file == null) {
            return "/dev/null";
        }
        return file.getAbsolutePath();
    }

    public File getOutputFile() {
        return file;
    }

    @Override
    public void resetOutput(String path) throws IOException {
        file = new File(path);
        resetOutput();
    }

    @Override
    public void resetOutput() throws IOException {
        if (file == null) {
            createTempFile();
        }
        closeOutput();
        outputStream = new XStream().createObjectOutputStream(new FileWriter(file));
        for (String name : SimonManager.simonNames()) {
            SimonManager.getSimon(name).reset();
        }
    }

    public void flushOuput() throws IOException {
        outputStream.flush();
    }

    @Override
    public void closeOutput() throws IOException {
        if (outputStream == null) {
            return;
        }
        outputStream.close();
        outputStream = null;
    }

    private void createTempFile() throws IOException {
        file = File.createTempFile("nx-samples-", ".xml");
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public long getLastUsage() {
        return lastUsage;
    }

}
