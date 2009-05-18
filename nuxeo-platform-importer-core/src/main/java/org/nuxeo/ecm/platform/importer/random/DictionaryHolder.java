package org.nuxeo.ecm.platform.importer.random;

public interface DictionaryHolder {

    public int getWordCount();

    public String getRandomWord();

    public void init() throws Exception;
}