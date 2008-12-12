package org.nuxeo.ecm.platform.commandline.executor.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "environment")
public class EnvironementDescriptor implements Serializable{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNode("workingDirectory")
    protected String workingDirectory;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();


    public String getWorkingDirectory()
    {
        if (workingDirectory==null) {
            workingDirectory = System.getProperty("java.io.tmpdir");
        }
        if (!workingDirectory.endsWith("/")) {
            workingDirectory = workingDirectory + "/";
        }
        return workingDirectory;
    }


    public void merge(EnvironementDescriptor other) {
        if (other.workingDirectory!=null) {
            this.workingDirectory = other.workingDirectory;
        }
        this.parameters.putAll(other.parameters);
    }


}
