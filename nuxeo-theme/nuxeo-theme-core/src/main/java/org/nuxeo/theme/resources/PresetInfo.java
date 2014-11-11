package org.nuxeo.theme.resources;

public class PresetInfo {

    private String name;

    private String bank;

    private String collection;

    private String category;

    private String value;

    public PresetInfo(String name, String bank, String collection,
            String category, String value) {
        this.name = name;
        this.bank = bank;
        this.collection = collection;
        this.category = category;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
