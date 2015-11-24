package info.simplecloud.scimproxy.compliance.enteties;

import info.simplecloud.scimproxy.compliance.enteties.TestResult;

public class ReadableTestResult extends TestResult {

    public ReadableTestResult(TestResult result) {
        super(result.getStatus(), result.name, result.message, result.wire);
    }
        
    public boolean isFailed() {
        return getStatus()==TestResult.ERROR;
    }
    
    public String getDisplay() {
        return this.statusText + ": " + this.name;
    }
    
    public String getErrorMessage() {
        return this.message;
    }        
}
