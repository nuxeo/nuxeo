package org.nuxeo.opensocial.container.shared.webcontent.abs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public abstract class AbstractWebContentData implements Serializable,
        Comparable<AbstractWebContentData>, WebContentData {
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String title;

    private String unitId;

    private long position;

    private String owner;

    private String viewer;

    private boolean isInAPortlet = true;

    private boolean isCollapsed = false;

    private Map<String, String> preferences;

    private Map<String, String> additionalPreferences;

    private transient List<Serializable> files;

    public AbstractWebContentData() {
        preferences = new HashMap<String, String>();
        additionalPreferences = new HashMap<String, String>();
        files = new ArrayList<Serializable>();
    }

    public boolean isCollapsed() {
        return this.isCollapsed;
    }

    public void setIsCollapsed(boolean isCollapsed) {
        this.isCollapsed = isCollapsed;
    }

    public boolean isInAPorlet() {
        return isInAPortlet;
    }

    public void setIsInAPortlet(boolean isInAPortlet) {
        this.isInAPortlet = isInAPortlet;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public int compareTo(AbstractWebContentData webContent) {
        long pos1 = webContent.getPosition();
        long pos2 = this.getPosition();

        return (int) (pos2 - pos1);
    }

    public void addPreference(String pref, String value) {
        this.preferences.put(pref, value);
    }

    public void setPreferences(Map<String, String> preferences) {
        this.preferences = preferences;
    }

    public Map<String, String> getPreferences() {
        return preferences;
    }

    @Override
    public Map<String, String> getAdditionalPreferences() {
        return additionalPreferences;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setViewer(String viewer) {
        this.viewer = viewer;
    }

    public String getViewer() {
        return viewer;
    }

    public boolean initPrefs(Map<String, String> params) {
        if (params.get(WC_TITLE_PREFERENCE) != null) {
            setTitle(params.get(WC_TITLE_PREFERENCE));
        }

        return true;
    }

    abstract public String getAssociatedType();

    abstract public String getIcon();

    public void addFile(Serializable file) {
        files.add(file);
    }

    public void clearFiles() {
        files.clear();
    }

    public List<Serializable> getFiles() {
        return files;
    }

}
