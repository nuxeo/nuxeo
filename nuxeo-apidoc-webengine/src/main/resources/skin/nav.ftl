<#if Root.currentDistribution!=null>
<table border=0>
<tr>
      <td colspan="4" style="font-weight:bold">
      ${Root.currentDistribution.key} <A href="${Root.path}"> change</A>
      </td>
</tr>
<tr>
      <td colspan="4" style="font-weight:bold">
      Listings
      </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td colspan="3">
      <A href="${Root.path}/${distId}/listBundleGroups">
       List Bundle groups
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td colspan="3">
      <A href="${Root.path}/${distId}/listBundles">
       List bundles
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td colspan="3">
      <A href="${Root.path}/${distId}/listComponents">
       List components
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td colspan="3">
      <A href="${Root.path}/${distId}/listServices">
       List services
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td colspan="3">
      <A href="${Root.path}/${distId}/listExtensionPoints">
       List ExtensionPoints
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td colspan="3">
      <A href="${Root.path}/${distId}/listContributions">
       List Contributions
      </A>
    </td>
</tr>
</table>
</#if>