package org.nuxeo.apidoc.documentation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.AbstractDocumentationItem;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;

import com.cforcoding.jmd.MarkDownParserAndSanitizer;

public class ResourceDocumentationItem extends AbstractDocumentationItem
        implements DocumentationItem {

    protected String content;

    protected String filename;

    protected NuxeoArtifact target;

    protected String type;

    public ResourceDocumentationItem(String filename, String content,
            NuxeoArtifact target, String type) {
        this.content = content;
        this.filename = filename;
        this.target = target;
        this.type = type;
    }

    @Override
    public String getTitle() {
        return filename;
    }

    @Override
    public String getContent() {
        MarkDownParserAndSanitizer parser = new MarkDownParserAndSanitizer();
        String xHtml = parser.transform(content);
        return xHtml;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getRenderingType() {
        return "html";
    }

    @Override
    public List<String> getApplicableVersion() {
        return Arrays.asList(target.getVersion());
    }

    @Override
    public String getTarget() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTargetType() {
        return target.getArtifactType();
    }

    @Override
    public boolean isApproved() {
        return true;
    }

    @Override
    public String getId() {
        return getTargetType() + "--" + filename;
    }

    @Override
    public String getUUID() {
        return null;
    }

    @Override
    public Map<String, String> getAttachments() {
        return new HashMap<String, String>();
    }

    @Override
    public boolean isPlaceHolder() {
        return false;
    }

    @Override
    public String getEditId() {
        return null;
    }

}
