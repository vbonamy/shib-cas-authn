## NOTE Documentation and release artifacts are being worked on. As such documenatation and/or artifacts may not match, we thank you for your patience!

## A Shibboleth IdP v5.X plugin for delegating authentication to an external SSO Server using the CAS protocol


This is a Shibboleth IdP external authentication plugin that delegates primary authentication to an external 
Single Sign On Server using the Central Authentication Server protocol. The biggest advantage of using this component over the plain 
`REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range 
of native CAS protocol features such as `renew` and `gateway`, plus the ability to share with CAS the 
EntityID of the relying application.

The plugin takes advantage of and extends the Shibboleth IdP's external authentication flow, and consists of a number of JAR artifacts that bridge the gap between Shibboleth and CAS.

Maintenance Status
-------------------------------------------------------------

Maintenance of this project is sponsored by Unicon's [Open Source Support program](https://unicon.net/support). Professional support/integration assistance for this module is available. For more information, visit <https://unicon.net/opensource/shibboleth>.

Also, please do note that the Shibboleth IdP v3x+ has support for the CAS protocol and Apereo CAS server v5+ also has support for the SAML2 protocol. Unless justified otherwise, a better approach long-term would be to consolidate down to one platform removing the need to deploy and configure this plugin.


Software Requirements
-------------------------------------------------------------

This minimum supported version of Shibboleth Identity Provider is `5.0.0`. 
See [releases](https://github.com/Renater/shib-cas-authn/releases) to find the the appropriate version.


Installation
---------------------------------------------------------------

#### Overview

- Download and extract the "latest release" zip or tar [from releases](https://github.com/Unicon/shib-cas-authn/releases).
- Copy the no-conversation-state.jsp file (also found inside this repo in IDP_HOME/edit-webapp) to your IdP's `IDP_HOME/edit-webapp`
- Copy two included jar files (`cas-client-core-4.x.x.jar` and `shib-cas-athenticator-5.x.x.jar`) into the `IDP_HOME/edit-webapp/WEB-INF/lib`.
- Update the IdP's `authn.properties` file.
- (Optional) Update the IdP's `authn.properties` file.
- Update the IdP's `idp.properties` file.
- Rebuild the war file.

**NOTE:** You should **ALWAYS** refers to the `README.md` file that is [packaged with the release](https://github.com/Unicon/shib-cas-authn/releases) for instructions.


#### Update the IdP's authn.properties file

In the `IDP_HOME/conf/authn/authn.properties` file, ensure the context path points to `Authn/External` as shown below.

```
   # Servlet context-relative path to wherever your implementation lives
   idp.authn.External.externalAuthnPath = contextRelative:Authn/External
```


#### OPTIONAL Update the IdP's authn.properties file

You may also need to ensure the `authn/External` flow is able to accept passive and forced authentication if you wish to use those features. The `authn/External` bean is modified in the `IDP_HOME/authn/general-authn.xml` file as shown below. Note that non browser flow is not possible or supported so it should be false.

```
idp.authn.External.passiveAuthenticationSupported = true
idp.authn.External.nonBrowserSupported = false
idp.authn.External.forcedAuthenticationSupported = true
```


#### Update the IdP's idp.properties file

1. Set the `idp.authn.flows` to `External` in `IDP_HOME/conf/idp.properties`. Or, for advance cases, add `External` to the list or configure the MFA plug-in if you have others.
1. Add new properties for the ShibCas plugin.

```properties   
...
# Regular expression matching login flows to enable, e.g. IPAddress|Password
#idp.authn.flows = Password
idp.authn.flows = External

# CAS Client properties (usage loosely matches that of the Java CAS Client)
## CAS Server Properties
shibcas.casServerUrlPrefix = https://cassserver.example.edu/cas
shibcas.casServerLoginUrl = ${shibcas.casServerUrlPrefix}/login

## Shibboleth Server Properties
shibcas.serverName = https://shibserver.example.edu

# By default AuthenticatedNameTranslator, CasRefedsAuthnMethodTranslator and CasRefedsAuthnMethodParameterBuilder are always active.
# To add extra translators (semicolon-separated fully qualified class names):
# shibcas.casToShibTranslators = com.your.institution.MyCustomNamedTranslatorClass
# To replace the default parameter builders (semicolon-separated fully qualified class names):
# shibcas.parameterBuilders = com.your.institution.MyParameterBuilderClass

# Specify CAS validator to use - either 'cas10', 'cas20' or 'cas30' (default)
# shibcas.ticketValidatorName = cas30


# Specify if the Relying Party/Service Provider entityId should be appended as a separate entityId query string parameter
# or embedded in the "service" querystring parameter - `append` (default) or `embed`
# shibcas.entityIdLocation = append
# CAS MFA provider name used for REFEDS MFA profile support (see section below).
# If not set, the REFEDS MFA translator is loaded but inactive (no-op).
# shibcas.casAuthnContextClass = mfa-esupotp
...
```


#### Rebuild the war file

From the `IDP_HOME/bin` directory, run `./build.sh` or `build.bat` to rebuild the `idp.war`. Redeploy if necessary.


#### OPTIONAL EntityId / CAS Service Passing
By setting `shibcas.entityIdLocation=embed`, shib-cas-authn will embed the entityId in the service string so that CAS Server
can use the entityId when evaluating a service registry entry match. Using serviceIds of something like: 
`https://shibserver.example.edu/idp/Authn/ExtCas\?conversation=[a-z0-9]*&entityId=http://testsp.school.edu/sp`
or
`https://shibserver.example.edu/idp/Authn/ExtCas\?conversation=[a-z0-9]*&entityId=http://test.unicon.net/sp`
will match as two different entries in the service registry which will allow as CAS admin to enable MFA or use access strategies on an SP by SP basis. 


OPTIONAL Handling REFEDS MFA Profile
---------------------------------------------------------------

The plugin has native support for the [REFEDS MFA profile](https://refeds.org/profile/mfa). The requested authentication context class `https://refeds.org/profile/mfa`
is passed from the Shibboleth IdP to this plugin and translated into a multifactor authentication strategy supported by CAS.
The CAS server is notified of the required authentication method via a special `authn_method` parameter appended to the CAS login URL.
Once a service ticket is issued, the plugin validates that the CAS assertion contains and asserts the expected authentication context class.

Any CAS MFA provider is supported (e.g. `mfa-simple`, `mfa-esupotp`, `mfa-duo`, `mfa-gauth`, `mfa-yubikey`, …); configure the provider name via `shibcas.casAuthnContextClass`.

The two components involved are symmetric:

| Direction | Class | Role |
|-----------|-------|------|
| IdP → CAS (request) | `CasRefedsAuthnMethodParameterBuilder` | appends `&authn_method=<value>` to the CAS login URL |
| CAS → IdP (response) | `CasRefedsAuthnMethodTranslator` | translates the CAS assertion's `authnContextClass` back to the REFEDS URI |

Both read the same `shibcas.casAuthnContextClass` property and are both a **no-op** when it is absent, ensuring consistent behaviour in either direction.

#### REFEDS MFA Profile Configuration

The REFEDS translator and parameter builder are **loaded by default** but remain **inactive (no-op)** until `shibcas.casAuthnContextClass` is set.
A message is logged at `INFO` level at startup when the property is missing.

In `IDP_HOME/conf/idp.properties`, set the CAS MFA provider name:

```properties
# If not set, the REFEDS MFA translator is loaded but inactive (no-op).
shibcas.casAuthnContextClass = mfa-esupotp
```

Then add the desired authn context refs to `authn/External` in `authn/general-authn.xml`:

```xml
<bean id="authn/External" parent="shibboleth.AuthenticationFlow"
      p:passiveAuthenticationSupported="true"
      p:forcedAuthenticationSupported="true"
      p:nonBrowserSupported="false">
  <property name="supportedPrincipals">
    <list>
      <bean parent="shibboleth.SAML2AuthnContextClassRef" c:classRef="https://refeds.org/profile/mfa"/>
      <bean parent="shibboleth.SAML2AuthnContextClassRef" c:classRef="urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"/>
      <bean parent="shibboleth.SAML2AuthnContextClassRef" c:classRef="urn:oasis:names:tc:SAML:2.0:ac:classes:Password"/>
      <bean parent="shibboleth.SAML1AuthenticationMethod"  c:method="urn:oasis:names:tc:SAML:1.0:am:password"/>
    </list>
  </property>
</bean>
```

#### REFEDS MFA Profile Configuration on Shibboleth SP

This part of documentation is out of scope of the plugin but provided here for convenience.
The SP 
* needs to request the `https://refeds.org/profile/mfa` authentication context class ref for the IdP to trigger the CAS MFA flow.
* and must verify that the AuthnContextClassRef in the SAML assertion matches the requested context class, it reject responses that do not meet this requirement.

For example : 
```
  <Location />
     AuthType shibboleth
     ShibRequestSetting requireSession 1
     # request the REFEDS MFA profile authn context class ref to trigger the CAS MFA flow in the plugin
     ShibRequestSetting authnContextClassRef https://refeds.org/profile/mfa
     <RequireAll>
      # verify that the CAS assertion contains the expected authn context class ref, otherwise reject the response
      require authnContextClassRef https://refeds.org/profile/mfa
      require valid-user
     </RequireAll>
     # error page when the assertion does not contain the expected authn context class ref
     # (e.g. because the user did not complete MFA, because of a misconfiguration, because url without the authn_method parameter, ...)
     ErrorDocument 401 /errors/mfa-is-required.html
  </Location>
  <Location /errors>
     AuthType none
     require all granted
  </Location>
```

Note that if the IdP selected for authentication (chosen on the Discovery Service, for example) does not support the REFEDS MFA profile, 
the SP will directly reject the user by redirecting them to an `opensaml::FatalProfileException` error page. 
Therefore, if you know that some IdPs in your federation do not support the REFEDS MFA profile, 
you may want to provide a more user-friendly error page by setting the `<Errors>` element in shibboleth2.xml.

For example : 
```
<Errors supportContact="support@example.org"
        redirectErrors="https://sp-mfa.example.org/errors/saml-error.html" />
```
With saml-error.html like :
```
<!DOCTYPE html>
<html>
  <body>
    <h1>Authentication Error</h1>
    <div id="msg">An error occurred during authentication.</div>
    <script>
      const p = new URLSearchParams(window.location.search);
      const sub = p.get('statusCode2') || '';
      if (sub.includes('NoAuthnContext')) {
        document.getElementById('msg').textContent =
          "Your institution does not support the Multi-Factor Authentication (MFA) required to access this service. Please contact your IT support.";
      }
    </script>
  </body>
</html>
```

Release Notes
-------------------------------------------------------------
See [here](https://github.com/renater/shib-cas-authn/releases/).

Developer Notes
-------------------------------------------------------------
The project distributables can be built using `./gradlew clean build`. The artifacts will be in `build/distributions`.

This project includes a Docker environment to assist with development/testing. 

To build and execute: `./gradlew clean; ./gradlew up`
Then browse to: `https://idptestbed/idp/profile/SAML2/Unsolicited/SSO?providerId=https://sp.idptestbed/shibboleth`

> You'll need a `hosts` file entry that points `idptestbed` to your Docker server's IP address. 

The IdP only has a session of 1 minute (to test expired session/conversation key issues), so login into CAS Server quickly.
