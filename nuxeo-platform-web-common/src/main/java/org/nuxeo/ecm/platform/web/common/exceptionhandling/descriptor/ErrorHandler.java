package org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject
public class ErrorHandler {
    @XNode("@error")
    private String error;

    @XNode("@message")
    private String message;

    @XNode("@page")
    private String page;

    @XNode("@code")
    private Integer code;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

}