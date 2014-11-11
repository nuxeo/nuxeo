package org.nuxeo.ecm.platform.audit.ws;

public class BatchInfo {

    private int pageSize=1000;

    private int nextPage=1;

    private String initialDateRange=null;

    public BatchInfo(String dateRange)
    {
        this.initialDateRange=dateRange;
    }

    public String getPageDateRange() {
        return initialDateRange;
    }

    public int getNextPage() {
        return nextPage;
    }

    public void setNextPage(int nextPage) {
        this.nextPage = nextPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void prepareNextCall()
    {
        nextPage+=1;
    }
}
