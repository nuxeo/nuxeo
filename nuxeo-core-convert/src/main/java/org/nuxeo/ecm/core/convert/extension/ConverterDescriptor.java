package org.nuxeo.ecm.core.convert.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("converter")
public class ConverterDescriptor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected Converter instance = null;

    @XNode("@name")
    protected String converterName;

    @XNodeList(value = "sourceMimeType", type = ArrayList.class, componentType = String.class)
    protected List<String> sourceMimeTypes = new ArrayList<String>();

    @XNode("destinationMimeType")
    protected String destinationMimeType;

    @XNode("@class")
    private Class className;

    @XNodeList(value = "conversionStep", type = ArrayList.class, componentType = String.class)
    protected List<String> steps = new ArrayList<String>();

    public String getConverterName() {
        return converterName;
    }

    public List<String> getSourceMimeTypes() {
        return sourceMimeTypes;
    }

    public List<String> getSteps() {
        return steps;
    }

    public String getDestinationMimeType() {
        return destinationMimeType;
    }

    public void initConverter() throws Exception {
        if (instance == null) {
            if (className == null) {
                instance = new ChainedConverter();
            }
            else {
                instance = (Converter) className.newInstance();
            }
            instance.init(this);
        }
    }

    public Converter getConverterInstance() {
        try {
            initConverter();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return instance;
    }
}
