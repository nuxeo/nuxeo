<@extends src="base.ftl">

<@block name="right">
<h1> Browsing ${Root.currentDistribution.key} distribution </h1>

<div class="tabscontent">

  <ul class="apibrowser">
    <li class="bundles">
      <a href="${Root.path}/${distId}/listBundleGroups">Bundle groups</a>
      <ul>
        <li class="bundle">
          <a href="${Root.path}/${distId}/listBundles">Bundles</a>
          <ul>
            <li class="component">
              <a href="${Root.path}/${distId}/listComponents">Components</a>
              <ul>
                <li class="service">
                  <a href="${Root.path}/${distId}/listServices">Services</a>
                </li>
                <li class="extension">
                  <a href="${Root.path}/${distId}/listExtensionPoints">Extension points</a>
                </li>
                <li class="contribution">
                  <a href="${Root.path}/${distId}/listContributions">Contributions</a>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
  </ul>

</div>

</@block>

</@extends>
