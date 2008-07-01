package org.nuxeo.ecm.flex.javadto;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FlexDocumentModel implements Externalizable {


    private String docRef;
    private String name;
    private String parentPath;
    private String lifeCycleState;

    private transient Map<String, Map<String,Serializable>> data = new HashMap<String, Map<String,Serializable>>();


    public FlexDocumentModel()
    {
        docRef="docRef";
        name="docName";
        parentPath="/default/folder";
        lifeCycleState="work";
    }


    public void feed(String schemaName, Map<String,Serializable> map)
    {
        data.put(schemaName, map);
    }


    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {

        docRef=in.readUTF();
        name=in.readUTF();
        parentPath=in.readUTF();
        lifeCycleState=in.readUTF();

        //data = (Map<String, Map<String,Serializable>>) in.readObject();

    }

    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeUTF(docRef);
        out.writeUTF(name);
        out.writeUTF(parentPath);
        out.writeUTF(lifeCycleState);
        //out.writeObject(data);
    }

}
