package com.dhruv.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns a verified principal into a live HTTP session.
 *
 * <p>Credentials are checked by {@link com.dhruv.service.AuthService} against two separate
 * tables, so the standard {@code DaoAuthenticationProvider} does not fit. This class covers
 * the parts a form-login filter would otherwise handle: session-fixation defence, concurrent
 * session control, and persisting the security context.
 */
@Component
public class SessionAuthenticator {

    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
    private final SessionAuthenticationStrategy sessionStrategy;

    @Value("${server.servlet.session.cookie.name:JSESSIONID}")
    private String sessionCookieName;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean cookieSecure;

    public SessionAuthenticator(SessionRegistry sessionRegistry) {
        ConcurrentSessionControlAuthenticationStrategy concurrency =
                new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry);
        concurrency.setMaximumSessions(1);
        // Signing in on a second device expires the first session rather than being refused.
        concurrency.setExceptionIfMaximumExceeded(false);

        this.sessionStrategy = new CompositeSessionAuthenticationStrategy(List.of(
                concurrency,
                // Rotate the session id so a pre-authentication cookie cannot be replayed.
                new ChangeSessionIdAuthenticationStrategy(),
                new RegisterSessionAuthenticationStrategy(sessionRegistry)));
    }

    /**
     * Marks the current request as authenticated and writes the security context into the
     * session, so subsequent requests are authorised from the cookie alone.
     */
    public void establishSession(AppUserPrincipal principal,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null, // credentials are discarded once verified
                List.of(new SimpleGrantedAuthority(principal.getRole().authority())));

        sessionStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    /**
     * Expires the session cookie in the browser after logout. Invalidating the server-side
     * session is what actually revokes access; this stops the client re-sending a dead id.
     */
    public void clearSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(sessionCookieName, "");
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        response.addCookie(cookie);
    }
}
