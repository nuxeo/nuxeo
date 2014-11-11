package org.nuxeo.ecm.core.management.jtajca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.nuxeo.ecm.core.api.CoreInstance.RegistrationInfo;

public class Defaults {

    public static Defaults instance = new Defaults();

    public String name(Class<?> clazz) {
        return name(clazz, "default");
    }

    public String name(Class<?> clazz, String name) {
        return clazz.getPackage().getName() + ":type=" + clazz.getSimpleName()
                + ",name=" + name;
    }

    public ObjectName objectName(Class<?> clazz, String name) {
        try {
            return new ObjectName(name(clazz, name));
        } catch (MalformedObjectNameException e) {
            throw new UnsupportedOperationException("Cannot build  " + name, e);
        }
    }

    public String printStackTrace(RegistrationInfo info) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
            info.printStackTrace(new PrintStream(bos));
            return bos.toString();
        } catch (IOException e) {
            throw new Error("Cannot write stack to byte array", e);
        }
    }
}
