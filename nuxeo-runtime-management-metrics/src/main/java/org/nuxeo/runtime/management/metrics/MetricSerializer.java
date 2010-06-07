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
        for (Sample sample:samples) {
                outputStream.writeObject(sample);
        }
        count += 1;
        lastUsage = Calendar.getInstance().getTimeInMillis();
    }


    public String getOutputLocation() {
        if (file == null) {
            return "/dev/null";
        }
        return file.getAbsolutePath();
    }

    public File getOutputFile() {
        return file;
    }

    public void resetOutput(String path) throws IOException {
        file= new File(path);
        resetOutput();
    }

    public void resetOutput() throws IOException {
        if (file == null) {
            createTempFile();
        }
        closeOutput();
        outputStream = new XStream().createObjectOutputStream(new FileWriter(file));
        for (String name:SimonManager.simonNames()) {
            SimonManager.getSimon(name).reset();
        }
    }

    public void flushOuput() throws IOException {
        outputStream.flush();
    }

    public void closeOutput() throws IOException {
        if (outputStream == null) {
            return;
        }
        outputStream.close();
        outputStream= null;
    }

    private void createTempFile() throws IOException {
        file = File.createTempFile("nx-samples-", ".xml");
    }

    public int getCount() {
        return count;
    }

    public long getLastUsage() {
        return lastUsage;
    }


}
