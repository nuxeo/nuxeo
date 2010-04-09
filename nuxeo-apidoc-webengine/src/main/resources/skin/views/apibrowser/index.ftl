<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<h1> Browsing ${Root.currentDistribution.key} distribution </h1>

<br/>
<br/>

<table border="0">
<tr>
    <td class="iconHolder"><img src="${skinPath}/images/NXBundle.png"/></td>
    <td class="linkHolder">
      <A href="${Root.path}/${distId}/listBundleGroups">
       Artifacts groups
      </A>
    </td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
</tr>
<tr>
    <td>&nbsp;</td>
    <td class="iconHolder"><img src="${skinPath}/images/NXBundle.png"/></td>
    <td class="linkHolder">
      <A href="${Root.path}/${distId}/listBundles">
       Bundles
      </A>
    </td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
</tr>
<tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td class="iconHolder"><img src="${skinPath}/images/NXComponent.png"/></td>
    <td class="linkHolder">
      <A href="${Root.path}/${distId}/listComponents">
       Components
    </td>
    <td>&nbsp;</td>
</tr>
<tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td class="iconHolder"><img src="${skinPath}/images/NXService.png"/></td>
    <td class="linkHolder">
      <A href="${Root.path}/${distId}/listServices">
       Services
      </A>
    </td>

</tr>
<tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td class="iconHolder"><img src="${skinPath}/images/NXExtensionPoint.png"/></td>
    <td class="linkHolder">
      <A href="${Root.path}/${distId}/listExtensionPoints">
       ExtensionPoints
      </A>
    </td>

</tr>
<tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td class="iconHolder"><img src="${skinPath}/images/NXContribution.png"/></td>
    <td class="linkHolder" colspan="2">
      <A href="${Root.path}/${distId}/listContributions">
       Contributions
      </A>
    </td>

</tr>
</table>

</@block>

</@extends>