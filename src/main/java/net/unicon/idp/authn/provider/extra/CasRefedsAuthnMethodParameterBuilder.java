package net.unicon.idp.authn.provider.extra;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.unicon.idp.ShibcasConstants;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Generates a querystring {@code authn_method} parameter when the SP requests the REFEDS MFA
 * profile ({@code https://refeds.org/profile/mfa}).
 *
 * <p>The CAS MFA provider name is read from the {@code shibcas.casAuthnContextClass} property
 * (e.g. {@code mfa-simple}, {@code mfa-esupotp}, {@code mfa-duo}, {@code mfa-gauth}, {@code mfa-yubikey}, …).
 * If the property is absent, this builder is a <strong>no-op</strong> — consistent with the
 * behaviour of {@link net.unicon.idp.externalauth.CasRefedsAuthnMethodTranslator} on the return path.
 *
 * <p>This class is used by default; no explicit {@code shibcas.parameterBuilders} configuration
 * is required unless you need a fully custom implementation.
 *
 * <p>To implement a custom parameter builder, implement {@link IParameterBuilder} directly and
 * register the class via {@code shibcas.parameterBuilders} (semicolon-separated).
 */
public class CasRefedsAuthnMethodParameterBuilder implements IParameterBuilder, ApplicationContextAware {


    private final Logger logger = LoggerFactory.getLogger(CasRefedsAuthnMethodParameterBuilder.class);

    /** Null means no-op — mirrors the default behaviour of CasRefedsAuthnMethodTranslator. */
    private String casAuthnMethod = null;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        final String value = applicationContext.getEnvironment()
            .getProperty(ShibcasConstants.PROPERTY_CAS_AUTHN_CONTEXT_CLASS);
        this.casAuthnMethod = (value != null && !value.isBlank()) ? value : null;
        if (this.casAuthnMethod == null) {
            logger.info("Property '{}' is not set — {} is loaded but will be a no-op. "
                + "Set the property to enable REFEDS MFA parameter forwarding to CAS.",
                ShibcasConstants.PROPERTY_CAS_AUTHN_CONTEXT_CLASS, getClass().getSimpleName());
        } else {
            logger.info("Property '{}' = '{}' — {} will append '&authn_method={}' for REFEDS MFA requests.",
                ShibcasConstants.PROPERTY_CAS_AUTHN_CONTEXT_CLASS, casAuthnMethod, getClass().getSimpleName(), casAuthnMethod);
        }
    }

    @Override
    public String getParameterString(final HttpServletRequest request, final String authenticationKey) {
        if (casAuthnMethod == null) {
            return "";
        }
        try {
            final ProfileRequestContext prc = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
            final AuthenticationContext authnContext = prc.ensureSubcontext(AuthenticationContext.class);
            final RequestedPrincipalContext principalCtx = authnContext.ensureSubcontext(RequestedPrincipalContext.class);
            if (principalCtx.getRequestedPrincipals().isEmpty()) {
                logger.debug("No requested principal context is available");
                return "";
            }
            final boolean refedsRequested = principalCtx.getRequestedPrincipals().stream()
                .anyMatch(p -> p.equals(new AuthnContextClassRefPrincipal(ShibcasConstants.REFEDS_MFA_URI)));
            if (refedsRequested) {
                logger.debug("REFEDS MFA profile requested — appending authn_method={}", casAuthnMethod);
                return "&authn_method=" + URLEncoder.encode(casAuthnMethod, StandardCharsets.UTF_8);
            }
            return "";
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            return "";
        }
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof CasRefedsAuthnMethodParameterBuilder;
    }
}
