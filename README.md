## A Shibboleth IdP v5.X plugin for delegating authentication to an external SSO Server using the CAS protocol


This is a Shibboleth IdP external authentication plugin that delegates primary authentication to an external
Single Sign On Server using the Central Authentication Server protocol. The biggest advantage of using this component over the plain
`REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range
of native CAS protocol features such as `renew` and `gateway`, plus the ability to share with CAS the
EntityID of the relying application.

The plugin takes advantage of and extends the Shibboleth IdP's external authentication flow, and consists of a number of JAR artifacts that bridge the gap between Shibboleth and CAS.

Maintenance Status
-------------------------------------------------------------

Please note that Unicon will no longer be upgrading nor maintaining this plugin any longer. That decision is based on best practices concerning the modern Shibboleth and CAS Server software packages. The Shibboleth IdP and the CAS Server support the same SSO protocols, and the best strategy is to decide which of those two SSO packages best meets your needs and only keep that one, migrating all services to it. If,for whatever reason, both SSO solutions are still required, the recommended approach is to use SAML (authentication) delegation to achieve the same results that this plugin provides now.

However, this fork of the plugin is maintained by [Renater]([Renater](https://www.renater.fr). It provides integration of Apereo CAS MFA features into the Shibboleth IdP via the REFEDS MFA profile, features unavailable in the original plugin and, to our knowledge, not (easily) achievable through SAML delegation either.

Software Requirements
-------------------------------------------------------------

This minimum supported version of Shibboleth Identity Provider is `5.2.1`.
See [releases](https://github.com/Renater/shib-cas-authn/releases) to find the the appropriate version.


Installation
---------------------------------------------------------------

#### Overview

- Download and extract the "latest release" zip or tar [from releases](https://github.com/Renater/shib-cas-authn/releases).
- Copy the no-conversation-state.jsp file (also found inside this repo in IDP_HOME/edit-webapp) to your IdP's `IDP_HOME/edit-webapp`
- Copy two included jar files (`cas-client-core-x.x.x.jar` and `shib-cas-authenticator-x.x.x.jar`) into the `IDP_HOME/edit-webapp/WEB-INF/lib`.
- Copy and Update the IdP's `web.xml`.
- Update the IdP's `authn.properties` file.
- Rebuild the war file.

**NOTE:** You should **ALWAYS** refers to the `README.md` file that is [packaged with the release](https://github.com/Renater/shib-cas-authn/releases) for instructions.

#### Update the IdP's authn.properties file

1. Set the `idp.authn.flows` to `External` in `IDP_HOME/conf/authn/authn.properties`. Or, for advance cases, add `External` to the list if you have others.
2. Add new properties for the ShibCas plugin.

```properties
...
idp.authn.flows = External

idp.authn.External.externalAuthnPath = contextRelative:Authn/External

shibcas.casServerUrlPrefix = https://cassserver.example.edu/cas
shibcas.casServerLoginUrl = ${shibcas.casServerUrlPrefix}/login

shibcas.serverName = https://shibserver.example.edu

# By default you always get the AuthenticatedNameTranslator, add additional code to cover your custom needs.
# Takes a comma separated list of fully qualified class names
# shibcas.casToShibTranslators = com.your.institution.MyCustomNamedTranslatorClass
# shibcas.parameterBuilders = com.your.institution.MyParameterBuilderClass

# Specify CAS validator to use - either 'cas10', 'cas20' or 'cas30' (default)
# shibcas.ticketValidatorName = cas30


# Specify if the Relying Party/Service Provider entityId should be appended as a separate entityId query string parameter
# or embedded in the "service" querystring parameter - `append` (default) or `embed`
# shibcas.entityIdLocation = append

idp.authn.Password.passiveAuthenticationSupported = true
idp.authn.Password.forcedAuthenticationSupported = true
idp.authn.External.nonBrowserSupported = false


...
```

#### Rebuild the war file

From the `IDP_HOME/bin` directory, run `./build.sh` or `build.bat` to rebuild the `idp.war`. Redeploy if necessary.


#### OPTIONAL EntityId / CAS Service Passing
By setting `shibcas.entityIdLocation=embed`, shib-cas-authn will embed the entityId in the service string so that CAS Server
can use the entityId when evaluating a service registry entry match. Using serviceIds of something like:
`https://shibserver\\.example\\.edu/idp/Authn/External\\?(?=(?:[^&]*&)*entityId=https://testsp\\.school\\.edu/sp([/&]|$)).*$`
or
`https://shibserver\\.example\\.edu/idp/Authn/External\\?(?=(?:[^&]*&)*entityId=https://testsp\\.renater\\.fr/sp([/&]|$)).*$`
will match as two different entries in the service registry which will allow as CAS admin to enable MFA or use access strategies on an SP by SP basis.


OPTIONAL Handling REFEDS MFA Profile
---------------------------------------------------------------

The plugin has native support for [REFEDS MFA profile](https://refeds.org/profile/mfa). The requested authentication context class that is `https://refeds.org/profile/mfa`
is passed along from the Shibboleth IdP over to this plugin and is then translated to a multifactor authentication strategy supported by and configured CAS (i.e. Duo Security).
The CAS server is notified of the required authentication method via a special `authn_method` parameter by default. Once a service ticket is issued and plugin begins to
validate the service ticket, it will attempt to ensure that the CAS-produced validation payload contains and can successfully assert the required/requested
authentication context class.

The supported multifactor authentication providers are listed below:

- MFA Simple (Requesting `authn_method=mfa-simple` and expecting validation payload attribute `authnContextClass=mfa-simple`)
- Duo Security (Requesting `authn_method=mfa-duo` and expecting validation payload attribute `authnContextClass=mfa-duo`)
- FIDO2 WebAuthn (Requesting `authn_method=mfa-webauthn` and expecting validation payload attribute `authnContextClass=mfa-webauthn`)
- Google Authenticator (Requesting `authn_method=mfa-gauth` and expecting validation payload attribute `authnContextClass=mfa-gauth`)
- Yubikey (Requesting `authn_method=mfa-yubikey` and expecting validation payload attribute `authnContextClass=mfa-yubikey`)
- Radius (Requesting `authn_method=mfa-radius` and expecting validation payload attribute `authnContextClass=mfa-radius`)
- Inwebo / Trustbuilder (Requesting `authn_method=mfa-inwebo` and expecting validation payload attribute `authnContextClass=mfa-inwebo`)
- ESUP-OTP (Requesting `authn_method=mfa-esupotp` and expecting validation payload attribute `authnContextClass=mfa-esupotp`)


#### REFEDS MFA Profile Configuration

In the `IDP_HOME/conf/authn/authn.properties` file, ensure the following settings are set:

```properties
shibcas.casToShibTranslators = net.unicon.idp.externalauth.CasMFARefedsAuthnMethodTranslator
shibcas.parameterBuilders = net.unicon.idp.authn.provider.extra.CasMultifactorRefedsToXXXXXAuthnMethodParameterBuilder
```

Finally add the authn context refs in the supported principals property list to in `IDP_HOME/conf/authn/authn.properties` as shown below.

```properties
idp.authn.External.supportedPrincipals = \
    saml2/urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport, \
    saml2/https://refeds.org/profile/mfa
```

#### REFEDS MFA Profile Configuration on Shibboleth SP

This part of documentation is out of scope of the plugin but provided here for convenience.
The SP
* needs to request the `https://refeds.org/profile/mfa` authentication context class ref for the IdP to trigger the CAS MFA flow.
* and must verify that the AuthnContextClassRef in the SAML assertion matches the requested context class, it reject responses that do not meet this requirement.

For example, with Apache and mod_shib, the SP configuration would look like this:
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
See [here](https://github.com/Renater/shib-cas-authn/releases/).

Developer Notes
-------------------------------------------------------------
The project distributables can be built using `./gradlew clean build`. The artifacts will be in `build/distributions`.

This project includes a Docker environment to assist with development/testing.

To build and execute: `./gradlew clean; ./gradlew up`
Then browse to: `https://idptestbed/idp/profile/SAML2/Unsolicited/SSO?providerId=https://sp.idptestbed/shibboleth`

> You'll need a `hosts` file entry that points `idptestbed` to your Docker server's IP address.

The IdP only has a session of 1 minute (to test expired session/conversation key issues), so login into CAS Server quickly.
