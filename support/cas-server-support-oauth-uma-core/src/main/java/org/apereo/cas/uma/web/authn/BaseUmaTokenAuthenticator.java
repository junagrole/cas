package org.apereo.cas.uma.web.authn;

import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.ticket.accesstoken.AccessToken;
import org.apereo.cas.ticket.registry.TicketRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;

import java.util.LinkedHashMap;

/**
 * This is {@link BaseUmaTokenAuthenticator}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseUmaTokenAuthenticator implements Authenticator<TokenCredentials> {
    private final TicketRegistry ticketRegistry;

    @Override
    public void validate(final TokenCredentials credentials, final WebContext webContext) {
        val token = credentials.getToken().trim();
        val at = this.ticketRegistry.getTicket(token, AccessToken.class);
        if (at == null || at.isExpired()) {
            val err = String.format("Access token is not found or has expired. Unable to authenticate requesting party access token %s", token);
            throw new CredentialsException(err);
        }
        if (!at.getScopes().contains(getRequiredScope())) {
            val err = String.format("Missing scope [%s]. Unable to authenticate requesting party access token %s", OAuth20Constants.UMA_PERMISSION_URL, token);
            throw new CredentialsException(err);
        }
        val profile = new CommonProfile();
        val authentication = at.getAuthentication();
        val principal = authentication.getPrincipal();
        profile.setId(principal.getId());
        val attributes = new LinkedHashMap<String, Object>(authentication.getAttributes());
        attributes.putAll(principal.getAttributes());

        profile.addAttributes(attributes);
        profile.addPermissions(at.getScopes());
        profile.addAttribute(AccessToken.class.getName(), at);

        LOGGER.debug("Authenticated access token [{}]", profile);
        credentials.setUserProfile(profile);
    }

    /**
     * Gets required scope.
     *
     * @return the required scope
     */
    protected abstract String getRequiredScope();
}
