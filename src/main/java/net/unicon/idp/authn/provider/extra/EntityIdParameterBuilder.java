package net.unicon.idp.authn.provider.extra;

import net.shibboleth.idp.authn.ExternalAuthentication;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Generates a querystring parameter containing the entityId
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
public class EntityIdParameterBuilder implements IParameterBuilder {

    @Override
    public String getParameterString(final HttpServletRequest request, final String authenticationKey) {
        return getParameterString(request, true);
    }

    public String getParameterString(final HttpServletRequest request, final boolean encode) {
        final String relayingPartyId = request.getAttribute(ExternalAuthentication.RELYING_PARTY_PARAM).toString();

        final String rpId;

        if (encode) {
            rpId = URLEncoder.encode(relayingPartyId, StandardCharsets.UTF_8);
        } else {
            rpId = relayingPartyId;
        }

        return "&entityId=" + rpId;
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof EntityIdParameterBuilder;
    }

}
