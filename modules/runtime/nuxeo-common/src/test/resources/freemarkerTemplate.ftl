test ${var1} and ${var2} and ${var3}
test ${var.decrypt.var1}
<#if "${var1}"?starts_with("value") >
test1 succeed
<#else>
test1 failed
</#if>
<#if "${decrypt(var1)}"?starts_with("value") >
test2 succeed
<#else>
test2 failed
</#if>
