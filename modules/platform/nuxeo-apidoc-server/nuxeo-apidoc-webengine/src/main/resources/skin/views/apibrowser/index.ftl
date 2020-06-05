<@extends src="base.ftl">

<@block name="right">
<h1> Browsing Distribution '${Root.currentDistribution.key}' </h1>

<div class="tabscontent">

  <ul class="apibrowser">
    <li class="bundlestree">
      <a href="${Root.path}/${distId}/listBundleGroups">Bundle Groups</a>
      <ul>
        <li class="bundletree">
          <a href="${Root.path}/${distId}/listBundles">Bundles</a>
          <ul>
            <li class="componenttree">
              <a href="${Root.path}/${distId}/listComponents">Components</a>
              <ul>
                <li class="servicetree">
                  <a href="${Root.path}/${distId}/listServices">Services</a>
                </li>
                <li class="extensiontree">
                  <a href="${Root.path}/${distId}/listExtensionPoints">Extension Points</a>
                </li>
                <li class="contributiontree">
                  <a href="${Root.path}/${distId}/listContributions">Contributions</a>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li class="operationstree">
      <a href="${Root.path}/${distId}/listOperations">Operations</a>
    </li>
    <li class="packagestree">
      <a href="${Root.path}/${distId}/listPackages">Packages</a>
    </li>
    <#list Root.getPluginMenu() as plugin>
      <li class="${plugin.getStyleClass()}tree">
        <a href="${Root.path}/${distId}/${plugin.getViewType()}/${plugin.getHomeView()}">${plugin.getLabel()}</a>
      </li>
    </#list>
  </ul>

</div>

</@block>

</@extends>
