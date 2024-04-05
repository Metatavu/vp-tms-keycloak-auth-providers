<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
   <#if section = "header">
       <#if autoLogin>
           <script type="text/javascript">
               setTimeout(function() {
                   document.getElementById('kc-form-login').submit();
               }, ${autoLoginInterval});
           </script>
       </#if>
   <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                        <#if hideTruckIdInput>
                            <input type="hidden" name="truckId" value="${truckId!''}"/>
                         <#else>
                            <input name="truckId" value="${truckId!''}"/>
                        </#if>

                        <#if !autoLogin>
                          <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <input type="submit" value="${msg("doSubmit")}"/>
                          </div>
                        </#if>
                    </form>
                </#if>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>