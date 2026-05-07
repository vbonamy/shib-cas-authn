package net.unicon.idp;

/**
 * Shared constants across shib-cas-authn components.
 */
public final class ShibcasConstants {

    /**
     * Spring property key for the CAS MFA provider name
     * (e.g. {@code mfa-simple}, {@code mfa-esupotp}, {@code mfa-duo}, {@code mfa-gauth}, {@code mfa-yubikey}, …).
     * Configure it in {@code IDP_HOME/conf/idp.properties}.
     */
    public static final String PROPERTY_CAS_AUTHN_CONTEXT_CLASS = "shibcas.casAuthnContextClass";

    /**
     * REFEDS MFA profile URI: {@value}
     */
    public static final String REFEDS_MFA_URI = "https://refeds.org/profile/mfa";

    private ShibcasConstants() {
        // utility class — no instantiation
    }
}
