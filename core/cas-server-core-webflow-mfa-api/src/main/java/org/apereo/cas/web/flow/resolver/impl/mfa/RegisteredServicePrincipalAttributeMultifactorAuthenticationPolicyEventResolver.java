package org.apereo.cas.web.flow.resolver.impl.mfa;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderSelector;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.web.flow.authentication.BaseMultifactorAuthenticationProviderEventResolver;
import org.apereo.cas.web.support.WebUtils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apereo.inspektr.audit.annotation.Audit;
import org.springframework.web.util.CookieGenerator;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * This is {@link RegisteredServicePrincipalAttributeMultifactorAuthenticationPolicyEventResolver}
 * that attempts to locate the given principal attribute in the service authentication policy
 * and match it against the pattern provided in the same policy.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
public class RegisteredServicePrincipalAttributeMultifactorAuthenticationPolicyEventResolver extends BaseMultifactorAuthenticationProviderEventResolver {


    public RegisteredServicePrincipalAttributeMultifactorAuthenticationPolicyEventResolver(
        final AuthenticationSystemSupport authenticationSystemSupport,
        final CentralAuthenticationService centralAuthenticationService,
        final ServicesManager servicesManager,
        final TicketRegistrySupport ticketRegistrySupport,
        final CookieGenerator warnCookieGenerator,
        final AuthenticationServiceSelectionPlan authenticationSelectionStrategies,
        final MultifactorAuthenticationProviderSelector selector) {
        super(authenticationSystemSupport, centralAuthenticationService, servicesManager, ticketRegistrySupport, warnCookieGenerator,
            authenticationSelectionStrategies, selector);
    }

    @Override
    public Set<Event> resolveInternal(final RequestContext context) {
        val service = resolveRegisteredServiceInRequestContext(context);
        val authentication = WebUtils.getAuthentication(context);

        if (authentication == null || service == null) {
            LOGGER.debug("No authentication or service is available to determine event for principal");
            return null;
        }

        val policy = service.getMultifactorPolicy();
        if (policy == null || service.getMultifactorPolicy().getMultifactorAuthenticationProviders().isEmpty()) {
            LOGGER.debug("Authentication policy is absent or does not contain any multifactor authentication providers");
            return null;
        }

        if (StringUtils.isBlank(policy.getPrincipalAttributeNameTrigger())
            || StringUtils.isBlank(policy.getPrincipalAttributeValueToMatch())) {
            LOGGER.debug("Authentication policy does not define a principal attribute and/or value to trigger multifactor authentication");
            return null;
        }

        val principal = authentication.getPrincipal();
        val providers = getAuthenticationProviderForService(service);
        return resolveEventViaPrincipalAttribute(principal,
            org.springframework.util.StringUtils.commaDelimitedListToSet(policy.getPrincipalAttributeNameTrigger()),
            service, context, providers, Pattern.compile(policy.getPrincipalAttributeValueToMatch()).asPredicate());
    }


    @Audit(action = "AUTHENTICATION_EVENT", actionResolverName = "AUTHENTICATION_EVENT_ACTION_RESOLVER",
        resourceResolverName = "AUTHENTICATION_EVENT_RESOURCE_RESOLVER")
    @Override
    public Event resolveSingle(final RequestContext context) {
        return super.resolveSingle(context);
    }
}
