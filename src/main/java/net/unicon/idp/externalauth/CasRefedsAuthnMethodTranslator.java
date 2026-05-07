package net.unicon.idp.externalauth;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import org.apereo.cas.client.validation.Assertion;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import net.unicon.idp.ShibcasConstants;

import javax.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates a CAS assertion's {@code authnContextClass} attribute into the REFEDS MFA profile
 * URI when the asserted value matches the configured CAS MFA provider name.
 *
 * <p>The CAS MFA provider name is read from the Spring {@link Environment} property
 * {@code shibcas.casAuthnContextClass} (e.g. {@code mfa-simple}, {@code mfa-esupotp}, {@code mfa-duo}, {@code mfa-gauth}, {@code mfa-yubikey}, …).
 *
 * <p>This translator is active by default but is a <strong>no-op</strong> when
 * {@code shibcas.casAuthnContextClass} is not set. Configure it in
 * {@code IDP_HOME/conf/idp.properties} to enable REFEDS MFA profile support:
 * <pre>
 * shibcas.casAuthnContextClass = mfa-esupotp
 * </pre>
 */
public class CasRefedsAuthnMethodTranslator implements CasToShibTranslator, EnvironmentAware {
    private final Logger logger = LoggerFactory.getLogger(CasRefedsAuthnMethodTranslator.class);


    private String casAuthnContextClass = null;

    @Override
    public void setEnvironment(final Environment environment) {
        this.casAuthnContextClass = environment.getProperty(ShibcasConstants.PROPERTY_CAS_AUTHN_CONTEXT_CLASS);
        if (this.casAuthnContextClass == null || this.casAuthnContextClass.isBlank()) {
            this.casAuthnContextClass = null;
            logger.info("Property '{}' is not set — {} translator is loaded but will be a no-op. "
                + "Set the property to enable REFEDS MFA profile support.", ShibcasConstants.PROPERTY_CAS_AUTHN_CONTEXT_CLASS, getClass().getSimpleName());
        } else {
            logger.info("CAS authn context class configured to [{}]"
                    + "The {} translator will translate CAS assertions with this authn context class to the REFEDS MFA profile.",
                casAuthnContextClass, getClass().getSimpleName());
        }
    }

    @Override
    public void doTranslation(final HttpServletRequest request, final HttpServletResponse response, final Assertion assertion, final String authenticationKey) throws Exception {
        if (casAuthnContextClass == null) {
            logger.info("{} is disabled (no '{}' property configured); skipping REFEDS MFA translation.",
                getClass().getSimpleName(), ShibcasConstants.PROPERTY_CAS_AUTHN_CONTEXT_CLASS);
            return;
        }

        final ProfileRequestContext prc = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
        final AuthenticationContext authnContext = prc.ensureSubcontext(AuthenticationContext.class);
        if (authnContext == null) {
            logger.debug("No authentication context is available");
            return;
        }
        final RequestedPrincipalContext principalCtx = authnContext.ensureSubcontext(RequestedPrincipalContext.class);
        if (principalCtx == null || principalCtx.getRequestedPrincipals().isEmpty()) {
            logger.debug("No requested principal context is available in the authentication context; Overriding class to {}", AuthnContext.PPT_AUTHN_CTX);
            overrideAuthnContextClass(AuthnContext.PPT_AUTHN_CTX, request, authenticationKey);
            return;
        }

        final Principal principal = new AuthnContextClassRefPrincipal(ShibcasConstants.REFEDS_MFA_URI);
        final Principal attribute = principalCtx.getRequestedPrincipals().stream().filter(p -> p.equals(principal)).findFirst().orElse(null);
        if (attribute == null) {
            logger.debug("No authn context class ref principal is found in the requested principals; overriding to {}", AuthnContext.PPT_AUTHN_CTX);
            overrideAuthnContextClass(AuthnContext.PPT_AUTHN_CTX, request, authenticationKey);
            return;
        }
        final String authnMethod = attribute.getName();
        logger.debug("Requested authn method provided by IdP is {}", authnMethod);
        if (!assertion.getPrincipal().getAttributes().containsKey("authnContextClass")) {
            logger.debug("No authentication context class is provided by CAS; Overriding context class to {}", AuthnContext.PPT_AUTHN_CTX);
            overrideAuthnContextClass(AuthnContext.PPT_AUTHN_CTX, request, authenticationKey);
            return;
        }

        final Object clazz = assertion.getPrincipal().getAttributes().get("authnContextClass");
        logger.debug("Located asserted authentication context class [{}]", clazz);

        if (casAuthnContextClass.equals(clazz)) {
            overrideAuthnContextClass(ShibcasConstants.REFEDS_MFA_URI, request, authenticationKey);
            logger.info("Validation payload successfully asserts the authentication context class for [{}]; Context class is set to {}", casAuthnContextClass, ShibcasConstants.REFEDS_MFA_URI);
            return;
        }
        logger.debug("Authentication context class [{}] provided by CAS does not match the configured value [{}]. "
            + "The requested authentication method to be used shall be {} and is left unmodified", clazz, casAuthnContextClass, authnMethod);
        overrideAuthnContextClass(clazz.toString(), request, authenticationKey);
    }

    private void overrideAuthnContextClass(final String clazz, final HttpServletRequest request, final String authenticationKey) throws Exception {
        final ProfileRequestContext prc = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
        final AuthenticationContext authnContext = prc.ensureSubcontext(AuthenticationContext.class);
        if (authnContext == null) {
            throw new IllegalArgumentException("No authentication method parameter is found in the request attributes");
        }
        final RequestedPrincipalContext principalCtx = authnContext.ensureSubcontext(RequestedPrincipalContext.class);
        logger.info("Overriding the principal authn context class ref to {}", clazz);
        if (principalCtx != null) {
            final List<Principal> principals = new ArrayList<>();
            final Principal principal = new AuthnContextClassRefPrincipal(clazz);
            principals.add(principal);
            principalCtx.setRequestedPrincipals(principals);
            principalCtx.setOperator("exact");
            principalCtx.setMatchingPrincipal(principal);

            principalCtx.getPrincipalEvalPredicateFactoryRegistry().register(AuthnContextClassRefPrincipal.class, "exact", new PrincipalEvalPredicateFactory() {
                @Nonnull
                @Override
                public PrincipalEvalPredicate getPredicate(@Nonnull final Principal candidate) {
                    return new PrincipalEvalPredicate() {

                        @Override
                        public boolean test(PrincipalSupportingComponent principalSupportingComponent) {
                            return principalSupportingComponent != null && principalSupportingComponent.getSupportedPrincipals(principal.getClass()).contains(principal);
                        }

                        @Override
                        public Principal getMatchingPrincipal() {
                            return principal;
                        }
                    };
                }
            });

            logger.info("The final requested authn context class ref principals are {}", principals);
        } else {
            logger.error("No requested principal context class is available");
        }
    }
}
