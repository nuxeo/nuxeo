/**
 * Normally served through the JsServet servlet of Apache Shindig.
 * Extracted from the following URL /nuxeo/opensocial/gadgets/js/rpc.js?c=1
 * for caching purpose
 *
 */

var gadgets=gadgets||{};
gadgets.config=function(){var A=[];
return{register:function(D,C,B){var E=A[D];
if(!E){E=[];
A[D]=E
}E.push({validators:C||{},callback:B})
},get:function(B){if(B){return configuration[B]||{}
}return configuration
},init:function(D,K){configuration=D;
for(var B in A){if(A.hasOwnProperty(B)){var C=A[B],H=D[B];
for(var G=0,F=C.length;
G<F;
++G){var I=C[G];
if(H&&!K){var E=I.validators;
for(var J in E){if(E.hasOwnProperty(J)){if(!E[J](H[J])){throw new Error('Invalid config value "'+H[J]+'" for parameter "'+J+'" in component "'+B+'"')
}}}}if(I.callback){I.callback(D)
}}}}},EnumValidator:function(E){var D=[];
if(arguments.length>1){for(var C=0,B;
(B=arguments[C]);
++C){D.push(B)
}}else{D=E
}return function(G){for(var F=0,H;
(H=D[F]);
++F){if(G===D[F]){return true
}}}
},RegExValidator:function(B){return function(C){return B.test(C)
}
},ExistsValidator:function(B){return typeof B!=="undefined"
},NonEmptyStringValidator:function(B){return typeof B==="string"&&B.length>0
},BooleanValidator:function(B){return typeof B==="boolean"
},LikeValidator:function(B){return function(D){for(var E in B){if(B.hasOwnProperty(E)){var C=B[E];
if(!C(D[E])){return false
}}}return true
}
}}
}();;
var gadgets=gadgets||{};
gadgets.log=function(A){gadgets.log.logAtLevel(gadgets.log.INFO,A)
};
gadgets.warn=function(A){gadgets.log.logAtLevel(gadgets.log.WARNING,A)
};
gadgets.error=function(A){gadgets.log.logAtLevel(gadgets.log.ERROR,A)
};
gadgets.setLogLevel=function(A){gadgets.log.logLevelThreshold_=A
};
gadgets.log.logAtLevel=function(D,C){if(D<gadgets.log.logLevelThreshold_||!gadgets.log._console){return
}var B;
var A=gadgets.log._console;
if(D==gadgets.log.WARNING&&A.warn){A.warn(C)
}else{if(D==gadgets.log.ERROR&&A.error){A.error(C)
}else{if(A.log){A.log(C)
}}}};
gadgets.log.INFO=1;
gadgets.log.WARNING=2;
gadgets.log.NONE=4;
gadgets.log.logLevelThreshold_=gadgets.log.INFO;
gadgets.log._console=window.console?window.console:window.opera?window.opera.postError:undefined;;
var tamings___=tamings___||[];
tamings___.push(function(A){___.grantRead(gadgets.log,"INFO");
___.grantRead(gadgets.log,"WARNING");
___.grantRead(gadgets.log,"ERROR");
___.grantRead(gadgets.log,"NONE");
caja___.whitelistFuncs([[gadgets,"log"],[gadgets,"warn"],[gadgets,"error"],[gadgets,"setLogLevel"],[gadgets.log,"logAtLevel"],])
});;
var gadgets=gadgets||{};
if(window.JSON&&window.JSON.parse&&window.JSON.stringify){gadgets.json={parse:function(B){try{return window.JSON.parse(B)
}catch(A){return false
}},stringify:function(B){try{return window.JSON.stringify(B)
}catch(A){return null
}}}
}else{gadgets.json=function(){function f(n){return n<10?"0"+n:n
}Date.prototype.toJSON=function(){return[this.getUTCFullYear(),"-",f(this.getUTCMonth()+1),"-",f(this.getUTCDate()),"T",f(this.getUTCHours()),":",f(this.getUTCMinutes()),":",f(this.getUTCSeconds()),"Z"].join("")
};
var m={"\b":"\\b","\t":"\\t","\n":"\\n","\f":"\\f","\r":"\\r",'"':'\\"',"\\":"\\\\"};
function stringify(value){var a,i,k,l,r=/["\\\x00-\x1f\x7f-\x9f]/g,v;
switch(typeof value){case"string":return r.test(value)?'"'+value.replace(r,function(a){var c=m[a];
if(c){return c
}c=a.charCodeAt();
return"\\u00"+Math.floor(c/16).toString(16)+(c%16).toString(16)
})+'"':'"'+value+'"';
case"number":return isFinite(value)?String(value):"null";
case"boolean":case"null":return String(value);
case"object":if(!value){return"null"
}a=[];
if(typeof value.length==="number"&&!value.propertyIsEnumerable("length")){l=value.length;
for(i=0;
i<l;
i+=1){a.push(stringify(value[i])||"null")
}return"["+a.join(",")+"]"
}for(k in value){if(k.match("___$")){continue
}if(value.hasOwnProperty(k)){if(typeof k==="string"){v=stringify(value[k]);
if(v){a.push(stringify(k)+":"+v)
}}}}return"{"+a.join(",")+"}"
}}return{stringify:stringify,parse:function(text){if(/^[\],:{}\s]*$/.test(text.replace(/\\["\\\/b-u]/g,"@").replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g,"]").replace(/(?:^|:|,)(?:\s*\[)+/g,""))){return eval("("+text+")")
}return false
}}
}()
};;
var tamings___=tamings___||[];
tamings___.push(function(A){caja___.whitelistFuncs([[gadgets.json,"parse"],[gadgets.json,"stringify"]])
});;
var gadgets=gadgets||{};
gadgets.util=function(){function G(L){var M;
var K=L;
var I=K.indexOf("?");
var J=K.indexOf("#");
if(J===-1){M=K.substr(I+1)
}else{M=[K.substr(I+1,J-I-1),"&",K.substr(J+1)].join("")
}return M.split("&")
}var E=null;
var D={};
var C={};
var F=[];
var A={0:false,10:true,13:true,34:true,39:true,60:true,62:true,92:true,8232:true,8233:true};
function B(I,J){return String.fromCharCode(J)
}function H(I){D=I["core.util"]||{}
}if(gadgets.config){gadgets.config.register("core.util",null,H)
}return{getUrlParameters:function(Q){if(E!==null&&typeof Q==="undefined"){return E
}var M={};
E={};
var J=G(Q||document.location.href);
var O=window.decodeURIComponent?decodeURIComponent:unescape;
for(var L=0,K=J.length;
L<K;
++L){var N=J[L].indexOf("=");
if(N===-1){continue
}var I=J[L].substring(0,N);
var P=J[L].substring(N+1);
P=P.replace(/\+/g," ");
M[I]=O(P)
}if(typeof Q==="undefined"){E=M
}return M
},makeClosure:function(L,N,M){var K=[];
for(var J=2,I=arguments.length;
J<I;
++J){K.push(arguments[J])
}return function(){var O=K.slice();
for(var Q=0,P=arguments.length;
Q<P;
++Q){O.push(arguments[Q])
}return N.apply(L,O)
}
},makeEnum:function(J){var L={};
for(var K=0,I;
(I=J[K]);
++K){L[I]=I
}return L
},getFeatureParameters:function(I){return typeof D[I]==="undefined"?null:D[I]
},hasFeature:function(I){return typeof D[I]!=="undefined"
},getServices:function(){return C
},registerOnLoadHandler:function(I){F.push(I)
},runOnLoadHandlers:function(){for(var J=0,I=F.length;
J<I;
++J){F[J]()
}},escape:function(I,M){if(!I){return I
}else{if(typeof I==="string"){return gadgets.util.escapeString(I)
}else{if(typeof I==="array"){for(var L=0,J=I.length;
L<J;
++L){I[L]=gadgets.util.escape(I[L])
}}else{if(typeof I==="object"&&M){var K={};
for(var N in I){if(I.hasOwnProperty(N)){K[gadgets.util.escapeString(N)]=gadgets.util.escape(I[N],true)
}}return K
}}}}return I
},escapeString:function(M){var J=[],L,N;
for(var K=0,I=M.length;
K<I;
++K){L=M.charCodeAt(K);
N=A[L];
if(N===true){J.push("&#",L,";")
}else{if(N!==false){J.push(M.charAt(K))
}}}return J.join("")
},unescapeString:function(I){return I.replace(/&#([0-9]+);/g,B)
}}
}();
gadgets.util.getUrlParameters();;
var tamings___=tamings___||[];
tamings___.push(function(A){caja___.whitelistFuncs([[gadgets.util,"escapeString"],[gadgets.util,"getFeatureParameters"],[gadgets.util,"hasFeature"],[gadgets.util,"registerOnLoadHandler"],[gadgets.util,"unescapeString"]])
});;
var gadgets=gadgets||{};
gadgets.rpctx=gadgets.rpctx||{};
gadgets.rpctx.wpm=function(){var A;
return{getCode:function(){return"wpm"
},isParentVerifiable:function(){return true
},init:function(B,C){A=C;
var D=function(E){B(gadgets.json.parse(E.data))
};
if(typeof window.addEventListener!="undefined"){window.addEventListener("message",D,false)
}else{if(typeof window.attachEvent!="undefined"){window.attachEvent("onmessage",D)
}}A("..",true);
return true
},setup:function(C,B){if(C===".."){gadgets.rpc.call(C,gadgets.rpc.ACK)
}return true
},call:function(C,F,E){var D=C===".."?window.parent:window.frames[C];
var B=gadgets.rpc.getOrigin(gadgets.rpc.getRelayUrl(C));
if(B){D.postMessage(gadgets.json.stringify(E),B)
}else{gadgets.error("No relay set (used as window.postMessage targetOrigin), cannot send cross-domain message")
}return true
}}
}();;
var gadgets=gadgets||{};
gadgets.rpctx=gadgets.rpctx||{};
gadgets.rpctx.frameElement=function(){var E="__g2c_rpc";
var B="__c2g_rpc";
var D;
var C;
function A(G,K,J){try{if(K!==".."){var F=window.frameElement;
if(typeof F[E]==="function"){if(typeof F[E][B]!=="function"){F[E][B]=function(L){D(gadgets.json.parse(L))
}
}F[E](gadgets.json.stringify(J));
return
}}else{var I=document.getElementById(G);
if(typeof I[E]==="function"&&typeof I[E][B]==="function"){I[E][B](gadgets.json.stringify(J));
return
}}}catch(H){}return true
}return{getCode:function(){return"fe"
},isParentVerifiable:function(){return false
},init:function(F,G){D=F;
C=G;
return true
},setup:function(J,F){if(J!==".."){try{var I=document.getElementById(J);
I[E]=function(K){D(gadgets.json.parse(K))
}
}catch(H){return false
}}if(J===".."){C("..",true);
var G=function(){window.setTimeout(function(){gadgets.rpc.call(J,gadgets.rpc.ACK)
},500)
};
gadgets.util.registerOnLoadHandler(G)
}return true
},call:function(F,H,G){A(F,H,G)
}}
}();;
var gadgets=gadgets||{};
gadgets.rpctx=gadgets.rpctx||{};
gadgets.rpctx.nix=function(){var C="GRPC____NIXVBS_wrapper";
var D="GRPC____NIXVBS_get_wrapper";
var F="GRPC____NIXVBS_handle_message";
var B="GRPC____NIXVBS_create_channel";
var A=10;
var J=500;
var I={};
var H;
var G=0;
function E(){var L=I[".."];
if(L){return
}if(++G>A){gadgets.warn("Nix transport setup failed, falling back...");
H("..",false);
return
}if(!L&&window.opener&&"GetAuthToken" in window.opener){L=window.opener;
if(L.GetAuthToken()==gadgets.rpc.getAuthToken("..")){var K=gadgets.rpc.getAuthToken("..");
L.CreateChannel(window[D]("..",K),K);
I[".."]=L;
window.opener=null;
H("..",true);
return
}}window.setTimeout(function(){E()
},J)
}return{getCode:function(){return"nix"
},isParentVerifiable:function(){return false
},init:function(L,M){H=M;
if(typeof window[D]!=="unknown"){window[F]=function(O){window.setTimeout(function(){L(gadgets.json.parse(O))
},0)
};
window[B]=function(O,Q,P){if(gadgets.rpc.getAuthToken(O)===P){I[O]=Q;
H(O,true)
}};
var K="Class "+C+"\n Private m_Intended\nPrivate m_Auth\nPublic Sub SetIntendedName(name)\n If isEmpty(m_Intended) Then\nm_Intended = name\nEnd If\nEnd Sub\nPublic Sub SetAuth(auth)\n If isEmpty(m_Auth) Then\nm_Auth = auth\nEnd If\nEnd Sub\nPublic Sub SendMessage(data)\n "+F+"(data)\nEnd Sub\nPublic Function GetAuthToken()\n GetAuthToken = m_Auth\nEnd Function\nPublic Sub CreateChannel(channel, auth)\n Call "+B+"(m_Intended, channel, auth)\nEnd Sub\nEnd Class\nFunction "+D+"(name, auth)\nDim wrap\nSet wrap = New "+C+"\nwrap.SetIntendedName name\nwrap.SetAuth auth\nSet "+D+" = wrap\nEnd Function";
try{window.execScript(K,"vbscript")
}catch(N){return false
}}return true
},setup:function(O,K){if(O===".."){E();
return true
}try{var M=document.getElementById(O);
var N=window[D](O,K);
M.contentWindow.opener=N
}catch(L){return false
}return true
},call:function(K,N,M){try{if(I[K]){I[K].SendMessage(gadgets.json.stringify(M))
}}catch(L){return false
}return true
}}
}();;
var gadgets=gadgets||{};
gadgets.rpctx=gadgets.rpctx||{};
gadgets.rpctx.rmr=function(){var G=500;
var E=10;
var H={};
var B;
var I;
function K(P,N,O,M){var Q=function(){document.body.appendChild(P);
P.src="about:blank";
if(M){P.onload=function(){L(M)
}
}P.src=N+"#"+O
};
if(document.body){Q()
}else{gadgets.util.registerOnLoadHandler(function(){Q()
})
}}function C(O){if(typeof H[O]==="object"){return
}var P=document.createElement("iframe");
var M=P.style;
M.position="absolute";
M.top="0px";
M.border="0";
M.opacity="0";
M.width="10px";
M.height="1px";
P.id="rmrtransport-"+O;
P.name=P.id;
var N=gadgets.rpc.getOrigin(gadgets.rpc.getRelayUrl(O))+"/robots.txt";
H[O]={frame:P,receiveWindow:null,relayUri:N,searchCounter:0,width:10,waiting:true,queue:[],sendId:0,recvId:0};
if(O!==".."){K(P,N,A(O))
}D(O)
}function D(N){var P=null;
H[N].searchCounter++;
try{if(N===".."){P=window.parent.frames["rmrtransport-"+gadgets.rpc.RPC_ID]
}else{P=window.frames[N].frames["rmrtransport-.."]
}}catch(O){}var M=false;
if(P){M=F(N,P)
}if(!M){if(H[N].searchCounter>E){return
}window.setTimeout(function(){D(N)
},G)
}}function J(N,P,T,S){var O=null;
if(T!==".."){O=H[".."]
}else{O=H[N]
}if(O){if(P!==gadgets.rpc.ACK){O.queue.push(S)
}if(O.waiting||(O.queue.length===0&&!(P===gadgets.rpc.ACK&&S&&S.ackAlone===true))){return true
}if(O.queue.length>0){O.waiting=true
}var M=O.relayUri+"#"+A(N);
try{O.frame.contentWindow.location=M;
var Q=O.width==10?20:10;
O.frame.style.width=Q+"px";
O.width=Q
}catch(R){return false
}}return true
}function A(N){var O=H[N];
var M={id:O.sendId};
if(O){M.d=Array.prototype.slice.call(O.queue,0);
M.d.push({s:gadgets.rpc.ACK,id:O.recvId})
}return gadgets.json.stringify(M)
}function L(X){var U=H[X];
var Q=U.receiveWindow.location.hash.substring(1);
var Y=gadgets.json.parse(decodeURIComponent(Q))||{};
var N=Y.d||[];
var O=false;
var T=false;
var V=0;
var M=(U.recvId-Y.id);
for(var P=0;
P<N.length;
++P){var S=N[P];
if(S.s===gadgets.rpc.ACK){I(X,true);
if(U.waiting){T=true
}U.waiting=false;
var R=Math.max(0,S.id-U.sendId);
U.queue.splice(0,R);
U.sendId=Math.max(U.sendId,S.id||0);
continue
}O=true;
if(++V<=M){continue
}++U.recvId;
B(S)
}if(O||(T&&U.queue.length>0)){var W=(X==="..")?gadgets.rpc.RPC_ID:"..";
J(X,gadgets.rpc.ACK,W,{ackAlone:O})
}}function F(P,S){var O=H[P];
try{var N=false;
N="document" in S;
if(!N){return false
}N=typeof S.document=="object";
if(!N){return false
}var R=S.location.href;
if(R==="about:blank"){return false
}}catch(M){return false
}O.receiveWindow=S;
function Q(){L(P)
}if(typeof S.attachEvent==="undefined"){S.onresize=Q
}else{S.attachEvent("onresize",Q)
}if(P===".."){K(O.frame,O.relayUri,A(P),P)
}else{L(P)
}return true
}return{getCode:function(){return"rmr"
},isParentVerifiable:function(){return true
},init:function(M,N){B=M;
I=N;
return true
},setup:function(O,M){try{C(O)
}catch(N){gadgets.warn("Caught exception setting up RMR: "+N);
return false
}return true
},call:function(M,O,N){return J(M,N.s,O,N)
}}
}();;
var gadgets=gadgets||{};
gadgets.rpctx=gadgets.rpctx||{};
gadgets.rpctx.ifpc=function(){var E=[];
var D=0;
var C;
function B(H){var F=[];
for(var I=0,G=H.length;
I<G;
++I){F.push(encodeURIComponent(gadgets.json.stringify(H[I])))
}return F.join("&")
}function A(I){var G;
for(var F=E.length-1;
F>=0;
--F){var J=E[F];
try{if(J&&(J.recyclable||J.readyState==="complete")){J.parentNode.removeChild(J);
if(window.ActiveXObject){E[F]=J=null;
E.splice(F,1)
}else{J.recyclable=false;
G=J;
break
}}}catch(H){}}if(!G){G=document.createElement("iframe");
G.style.border=G.style.width=G.style.height="0px";
G.style.visibility="hidden";
G.style.position="absolute";
G.onload=function(){this.recyclable=true
};
E.push(G)
}G.src=I;
window.setTimeout(function(){document.body.appendChild(G)
},0)
}return{getCode:function(){return"ifpc"
},isParentVerifiable:function(){return true
},init:function(F,G){C=G;
C("..",true);
return true
},setup:function(G,F){C(G,true);
return true
},call:function(F,K,I){var J=gadgets.rpc.getRelayUrl(F);
++D;
if(!J){gadgets.warn("No relay file assigned for IFPC");
return
}var H=null;
if(I.l){var G=I.a;
H=[J,"#",B([K,D,1,0,B([K,I.s,"","",K].concat(G))])].join("")
}else{H=[J,"#",F,"&",K,"@",D,"&1&0&",encodeURIComponent(gadgets.json.stringify(I))].join("")
}A(H);
return true
}}
}();;
var gadgets=gadgets||{};
gadgets.rpc=function(){var S="__cb";
var R="";
var G="__ack";
var Q=500;
var I=10;
var B={};
var C={};
var W={};
var J={};
var M=0;
var h={};
var V={};
var D={};
var e={};
var K={};
var U={};
var L=(window.top!==window.self);
var N=window.name;
var f=(function(){function i(j){return function(){gadgets.log("gadgets.rpc."+j+"("+gadgets.json.stringify(Array.prototype.slice.call(arguments))+"): call ignored. [caller: "+document.location+", isChild: "+L+"]")
}
}return{getCode:function(){return"noop"
},isParentVerifiable:function(){return true
},init:i("init"),setup:i("setup"),call:i("call")}
})();
if(gadgets.util){e=gadgets.util.getUrlParameters()
}var Z=(e.rpc_earlyq==="1");
function A(){return typeof window.postMessage==="function"?gadgets.rpctx.wpm:typeof window.postMessage==="object"?gadgets.rpctx.wpm:window.ActiveXObject?gadgets.rpctx.nix:navigator.userAgent.indexOf("WebKit")>0?gadgets.rpctx.rmr:navigator.product==="Gecko"?gadgets.rpctx.frameElement:gadgets.rpctx.ifpc
}function a(o,m){var k=b;
if(!m){k=f
}K[o]=k;
var j=U[o]||[];
for(var l=0;
l<j.length;
++l){var n=j[l];
n.t=X(o);
k.call(o,n.f,n)
}U[o]=[]
}function T(j){if(j&&typeof j.s==="string"&&typeof j.f==="string"&&j.a instanceof Array){if(J[j.f]){if(J[j.f]!==j.t){throw new Error("Invalid auth token. "+J[j.f]+" vs "+j.t)
}}if(j.s===G){window.setTimeout(function(){a(j.f,true)
},0);
return
}if(j.c){j.callback=function(k){gadgets.rpc.call(j.f,S,null,j.c,k)
}
}var i=(B[j.s]||B[R]).apply(j,j.a);
if(j.c&&typeof i!=="undefined"){gadgets.rpc.call(j.f,S,null,j.c,i)
}}}function d(k){if(!k){return""
}k=k.toLowerCase();
if(k.indexOf("//")==0){k=window.location.protocol+k
}if(k.indexOf("://")==-1){k=window.location.protocol+"//"+k
}var l=k.substring(k.indexOf("://")+3);
var i=l.indexOf("/");
if(i!=-1){l=l.substring(0,i)
}var n=k.substring(0,k.indexOf("://"));
var m="";
var o=l.indexOf(":");
if(o!=-1){var j=l.substring(o+1);
l=l.substring(0,o);
if((n==="http"&&j!=="80")||(n==="https"&&j!=="443")){m=":"+j
}}return n+"://"+l+m
}var b=A();
B[R]=function(){gadgets.warn("Unknown RPC service: "+this.s)
};
B[S]=function(j,i){var k=h[j];
if(k){delete h[j];
k(i)
}};
function O(k,i){if(V[k]===true){return
}if(typeof V[k]==="undefined"){V[k]=0
}var j=document.getElementById(k);
if(k===".."||j!=null){if(b.setup(k,i)===true){V[k]=true;
return
}}if(V[k]!==true&&V[k]++<I){window.setTimeout(function(){O(k,i)
},Q)
}else{K[k]=f;
V[k]=true
}}function F(j,m){if(typeof D[j]==="undefined"){D[j]=false;
var l=gadgets.rpc.getRelayUrl(j);
if(d(l)!==d(window.location.href)){return false
}var k=null;
if(j===".."){k=window.parent
}else{k=window.frames[j]
}try{D[j]=k.gadgets.rpc.receiveSameDomain
}catch(i){gadgets.error("Same domain call failed: parent= incorrectly set.")
}}if(typeof D[j]==="function"){D[j](m);
return true
}return false
}function H(j,i,k){C[j]=i;
W[j]=!!k
}function X(i){return J[i]
}function E(i,j){j=j||"";
J[i]=String(j);
O(i,j)
}function P(i){function k(n){var p=n?n.rpc:{};
var m=p.parentRelayUrl;
if(m.substring(0,7)!=="http://"&&m.substring(0,8)!=="https://"&&m.substring(0,2)!=="//"){if(typeof e.parent==="string"&&e.parent!==""){if(m.substring(0,1)!=="/"){var l=e.parent.lastIndexOf("/");
m=e.parent.substring(0,l+1)+m
}else{m=d(e.parent)+m
}}}var o=!!p.useLegacyProtocol;
H("..",m,o);
if(o){b=gadgets.rpctx.ifpc;
b.init(T,a)
}E("..",i)
}var j={parentRelayUrl:gadgets.config.NonEmptyStringValidator};
gadgets.config.register("rpc",j,k)
}function Y(k,i){var j=i||e.parent;
if(j){H("..",j);
E("..",k)
}}function c(i,m,o){if(!gadgets.util){return
}var l=document.getElementById(i);
if(!l){throw new Error("Cannot set up gadgets.rpc receiver with ID: "+i+", element not found.")
}var j=m||l.src;
H(i,j);
var n=gadgets.util.getUrlParameters(l.src);
var k=o||n.rpctoken;
E(i,k)
}function g(i,k,l){if(i===".."){var j=l||e.rpctoken||e.ifpctok||"";
if(gadgets.config){P(j)
}else{Y(j,k)
}}else{c(i,k,l)
}}if(L){g("..")
}return{register:function(j,i){if(j===S||j===G){throw new Error("Cannot overwrite callback/ack service")
}if(j===R){throw new Error("Cannot overwrite default service: use registerDefault")
}B[j]=i
},unregister:function(i){if(i===S||i===G){throw new Error("Cannot delete callback/ack service")
}if(i===R){throw new Error("Cannot delete default service: use unregisterDefault")
}delete B[i]
},registerDefault:function(i){B[R]=i
},unregisterDefault:function(){delete B[R]
},forceParentVerifiable:function(){if(!b.isParentVerifiable()){b=gadgets.rpctx.ifpc
}},call:function(i,j,o,m){i=i||"..";
var n="..";
if(i===".."){n=N
}++M;
if(o){h[M]=o
}var l={s:j,f:n,c:o?M:0,a:Array.prototype.slice.call(arguments,3),t:J[i],l:W[i]};
if(F(i,l)){return
}var k=K[i]?K[i]:b;
if(!k){if(!U[i]){U[i]=[l]
}else{U[i].push(l)
}return
}if(W[i]){k=gadgets.rpctx.ifpc
}if(k.call(i,n,l)===false){K[i]=f;
b.call(i,n,l)
}},getRelayUrl:function(j){var i=C[j];
if(i&&i.indexOf("//")==0){i=document.location.protocol+i
}return i
},setRelayUrl:H,setAuthToken:E,setupReceiver:g,getAuthToken:X,getRelayChannel:function(){return b.getCode()
},receive:function(i){if(i.length>4){T(gadgets.json.parse(decodeURIComponent(i[i.length-1])))
}},receiveSameDomain:function(i){i.a=Array.prototype.slice.call(i.a);
window.setTimeout(function(){T(i)
},0)
},getOrigin:d,init:function(){if(b.init(T,a)===false){b=f
}},ACK:G,RPC_ID:N}
}();
gadgets.rpc.init();;
