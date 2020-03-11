<html>
  <body>
    <div style="margin:0; padding:0; background-color:#e9ecef;font-family:Arial,sans-serif;" marginheight="0" marginwidth="0">
      <center>
        <table cellspacing="0" cellpadding="0" border="0" align="center" width="100%" height="100%" style="background-color:#e9ecef;border-collapse:collapse; font-family:Arial,sans-serif;margin:0; padding:0; min-height:100% ! important; width:100% ! important;border:none;">
          <tbody>
            <tr>
              <td align="center" valign="top" style="border-collapse:collapse;margin:0;padding:20px;border-top:0;min-height:100%!important;width:100%!important">
                <table cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;border:none;width:100%">
                  <tbody>
                    <tr>
                      <td style="background-color:#f7f7f7;border-bottom:1px dashed #e9ecef;padding:8px 20px;">
                        <p style="font-weight:bold;font-size:15px;margin:0;color:#000;">
                        ${Runtime.getProperty('org.nuxeo.ecm.product.name')}</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background-color:#fff;padding:8px 20px;"><br/>
                        <p style="line-height:180%;margin:0;font-size:13px;">
                      <#if eventId == "workflowNewProcessStarted">
                      A workflow was <strong>started</strong> on
                      <#elseif eventId == "workflowProcessEnded">
                      The workflow was <strong>ended</strong>
                      <#elseif eventId == "workflowAbandoned">
                      The workflow was <strong>abandoned<strong> for
                      <#elseif eventId == "workflowProcessCanceled">
                      The workflow was <strong>canceled</strong> for
                      <#elseif eventId == "workflowTaskAssigned">
                      A task was <strong>assigned</strong> for
                      <#elseif eventId == "workflowTaskUnassigned">
                      A task was <strong>unassigned</strong> for
                      <#elseif eventId == "workflowTaskCompleted">
                      A task <strong>ended</strong> for
                      <#elseif eventId == "workflowTaskRemoved">
                      A task was <strong>removed</strong> for
                      <#elseif eventId == "workflowTaskSuspended">
                      A task was <strong>suspended</strong> for
                      <#elseif eventId == "workflowTaskRejected">
                      A task was <strong>rejected</strong> for
                       </#if> ${htmlEscape(docTitle)}.</p><br/>
                        <table cellpadding="6" cellspacing="0" style="border:none;border-collapse:collapse;font-size:13px;">
                          <tbody>
                            <tr>
                              <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">By</td>
                              <td style="border:1px solid #eee;color:#000;font-size:13px;"><a href="${docUrl}" style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;">${htmlEscape(author)}</a>
                              </td>
                            </tr>
                            <tr>
                              <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">Date</td>
                              <td style="border:1px solid #eee;color:#000;font-size:13px;">${htmlEscape(dateTime?datetime?string("dd/MM/yyyy - HH:mm"))}
                              </td>
                            </tr>
                            <tr>
                              <td style="border:1px solid #eee;color:#888;font-size:13px;">Document identifier</td>
                              <td style="border:1px solid #eee;color:#000;font-size:13px;">${htmlEscape(docId)}</td>
                            </tr>
                          </tbody>
                        </table><br/>
                        <p style="margin:0;font-size:13px;">
                          <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;" href="${docUrl}">&#187; Consult the document ${htmlEscape(docTitle)}</a>
                        </p><br/>
                     </td>
                    </tr>
                    <tr>
                      <td style="background-color:#f7f7f7;border-top:1px dashed #e9ecef;text-align:center;padding:8px 20px;">
                        <div style="font-size:12px;color:#bbb;">
                        You received this notification because you subscribed to ${notification.name?lower_case} on this document or on one of its parents.</div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </td>
            </tr>
          </tbody>
        </table>
      </center>
    </div>
  </body>
<html>
