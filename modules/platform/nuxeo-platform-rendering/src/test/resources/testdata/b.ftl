
<@extends src="/testdata/a.ftl">

<@block name="a1">
 default content [ <@superBlock/> ]
 a1: Derived Block in b. Nesting another block:
 <@block name="b.nested">b.nested: Nested block defined in b</@block>
 <@block name="b.nested2">b.nested2: Nested block defined in b</@block>
</@block>


</@extends>
