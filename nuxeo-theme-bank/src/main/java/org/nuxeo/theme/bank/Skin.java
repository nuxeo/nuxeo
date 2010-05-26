package org.nuxeo.theme.bank;

public class Skin {

    private String bank;

    private String collection;

    private String name;

    public Skin(String bank, String collection, String name) {

        this.bank = bank;
        this.collection = collection;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
