<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
  <#if section = "header">
       ${msg("loginWithPinCodeTitle")}
   <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                  <#if hideDeviceIdInput>
                    <input type="hidden" name="deviceId" value="${deviceId!''}"/>
                  <#else>
                    <label for="deviceId">${msg("deviceId")}</label>
                    <input id="deviceId" name="deviceId" value="${deviceId!''}"/>
                  </#if>

                  <#if messagesPerField.existsError('clientApp')>
                      <span id="input-error-client-app-not-verified" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                          ${msg(messagesPerField.get('clientApp'))}
                      </span>
                  <#else>
                      <div class="vp-pin-code-container">
                        <label for="pin-code-input">${msg("pinCode")}</label>
                        <input id="pin-code-input" name="pinCode" type="password"/>

                        <#if messagesPerField.existsError('pinCode')>
                            <span id="input-error-pin-code" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${msg(messagesPerField.get('pinCode'))}
                            </span>
                        </#if>
                      </div>

                      <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                        <input type="submit" value="${msg("doLogIn")}"/>
                      </div>
                  </#if>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>