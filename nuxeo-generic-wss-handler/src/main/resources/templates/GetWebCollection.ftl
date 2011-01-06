<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <soap:Body>
        <GetWebCollectionResponse xmlns="http://schemas.microsoft.com/sharepoint/soap/">
            <GetWebCollectionResult>
                <Webs>
                    <#list sites as item>
                    <Web Title="${item.displayName}" Url="${request.getBaseUrl()}${item.getRelativeFilePath("")}" />
                    </#list>
                </Webs>
            </GetWebCollectionResult>
        </GetWebCollectionResponse>
    </soap:Body>
</soap:Envelope>