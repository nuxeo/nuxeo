<@extends src="base.ftl">

  <@block name="header_scripts">
    <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
    <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
  </@block>

  <@block name="left">
  </@block>

  <@block name="right">
  <h1> Your current Nuxeo Distribution is '${Root.currentDistribution.key}' </h1>

  <div class="tabscontent">

    <p>
      You can use this screen to browse your distribution.
    </p>

    <table id="stats">
      <tr>
        <td>Number of <a href="${Root.path}/${distId}/listBundles">Bundles</a></td>
        <td>${stats.bundles}</td>
      </tr>
      <tr>
        <td>Number of <a href="${Root.path}/${distId}/listComponents">Java Components</a></td>
        <td>${stats.jComponents}</td>
      </tr>
      <tr>
        <td>Number of <a href="${Root.path}/${distId}/listComponents">XML Components</a></td>
        <td>${stats.xComponents}</td>
      </tr>
      <tr>
        <td>Number of <a href="${Root.path}/${distId}/listServices">Services</a></td>
        <td>${stats.services}</td>
      </tr>
      <tr>
        <td>Number of <a href="${Root.path}/${distId}/listExtensionPoints">Extension Points</a></td>
        <td>${stats.xps}</td>
      </tr>
      <tr>
        <td>Number of <a href="${Root.path}/${distId}/listContributions">Contributions</a></td>
        <td>${stats.contribs}</td>
      </tr>
      <tr>
        <td>Number of <a href="${Root.path}/${distId}/listOperations">Operations</a></td>
        <td>${stats.operations}</td>
      </tr>
      <tr>
        <td>Number of <a href="${Root.path}/${distId}/listPackages">Packages</a></td>
        <td>${stats.packages}</td>
      </tr>
    </table>

    <#include "/docMacros.ftl">

    <h2>${bundleIds?size} bundles</h2>
    <@tableFilterArea "bundle"/>
    <table id="contentTable" class="tablesorter">
      <thead>
      <tr>
        <th>
          Bundle
        </th>
      </tr>
      </thead>
      <tbody>
        <#list bundleIds as bundleId>
        <#assign rowCss = (bundleId_index % 2 == 0)?string("even","odd")/>
        <tr class="${rowCss}">
          <td>
            <a href="${Root.path}/${distId}/viewBundle/${bundleId}" class="itemLink">${bundleId}</a>
          </td>
        </tr>
        </#list>
      </tbody>
    </table>

  </div>

  </@block>

  <@block name="footer_scripts">
    <@tableSortFilterScript "#contentTable" "[0,0]" />
  </@block>

</@extends>
