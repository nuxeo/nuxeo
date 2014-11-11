<h2>Audits</h2>

<#assign audits = Context.tail().getAudits() />

<table class="itemListing audits">
  <thead>
    <tr>
      <th>Id</th>
      <th>Occurred at</th>
      <th>Originated by</th>
    </tr>
  </thead>
  <tbody>
    <#list audits as audit>
    <tr>
      <td>${audit.eventId}</td>
      <td>${audit.eventDate}</td>
      <td>${audit.principalName}</td>
    </tr>
    </#list>
  </tbody>
</table>
