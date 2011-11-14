package org.nuxeo.wizard.download;

public class Preset {

    protected final String id;

    protected final String label;

    protected final String[] pkgs;

    public Preset(String id, String label, String[] pkgs) {
        this.id = id;
        this.label = label;
        this.pkgs = pkgs;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String[] getPkgs() {
        return pkgs;
    }

    public String getPkgsAsJsonArray() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < pkgs.length; i++) {
            if (i>0) {
                sb.append(",");
            }
            sb.append("'" + pkgs[i] + "'");
        }
        sb.append("]");
        return sb.toString();
    }


}
