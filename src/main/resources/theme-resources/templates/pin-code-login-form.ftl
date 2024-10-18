<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
   <#if section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                  <#if hideDeviceIdInput>
                    <input type="hidden" name="deviceId" value="${deviceId!''}"/>
                  <#else>
                    <label for="deviceId">${msg("deviceId")}</label>
                    <input id="deviceId" name="deviceId" value="${deviceId!''}"/>
                  </#if>

                  <label for="pinCode">${msg("pinCode")}</label>
                  <input id="pinCode" name="pinCode"/>

                  <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                    <input type="submit" value="${msg("doSubmit")}"/>
                  </div>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>