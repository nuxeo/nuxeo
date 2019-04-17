/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.management.metrics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.Calendar;

import org.javasimon.Sample;
import org.javasimon.SimonManager;

import org.nuxeo.runtime.api.Framework;

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
        @SuppressWarnings("resource") // Writer closed by outputStream.close()
        Writer writer = new FileWriter(file);
        outputStream = new XStream().createObjectOutputStream(writer);
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
        file = Framework.createTempFile("nx-samples-", ".xml");
        Framework.trackFile(file, file);
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
