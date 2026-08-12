package com.dhruv.controller;

import com.dhruv.domain.ParentEntity;
import com.dhruv.domain.StudentEntity;
import com.dhruv.dto.AuthenticatedUserDto;
import com.dhruv.dto.ParentAuthDto;
import com.dhruv.dto.StudentAuthDto;
import com.dhruv.repository.ParentRepository;
import com.dhruv.repository.StudentRepository;
import com.dhruv.security.AppRole;
import com.dhruv.security.AppUserPrincipal;
import com.dhruv.security.SessionAuthenticator;
import com.dhruv.service.AuthService;
import com.dhruv.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sign-in, sign-out, and session introspection.
 *
 * <p>Each successful authentication establishes a server-side session; the browser holds
 * only an opaque HttpOnly cookie. Previously the response body <em>was</em> the credential:
 * the client stored the returned user object in localStorage and the server verified
 * nothing on subsequent requests.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionAuthenticator sessionAuthenticator;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    public AuthController(AuthService authService,
                          SessionAuthenticator sessionAuthenticator,
                          StudentRepository studentRepository,
                          ParentRepository parentRepository) {
        this.authService = authService;
        this.sessionAuthenticator = sessionAuthenticator;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
    }

    @PostMapping("/student")
    public ResponseEntity<AuthenticatedUserDto> authenticateStudent(
            @Valid @RequestBody StudentAuthDto dto,
            HttpServletRequest request,
            HttpServletResponse response) {

        StudentEntity student = authService.authenticateStudent(dto);
        AppUserPrincipal principal = new AppUserPrincipal(
                student.getId(), student.getUserId(), student.getPhoneNumber(), AppRole.STUDENT);

        sessionAuthenticator.establishSession(principal, request, response);
        return ResponseEntity.ok(AuthenticatedUserDto.of(student, csrfToken(request)));
    }

    @PostMapping("/parent")
    public ResponseEntity<AuthenticatedUserDto> authenticateParent(
            @Valid @RequestBody ParentAuthDto dto,
            HttpServletRequest request,
            HttpServletResponse response) {

        ParentEntity parent = authService.authenticateParent(dto);
        AppUserPrincipal principal = new AppUserPrincipal(
                parent.getId(), parent.getUserId(), parent.getPhoneNumber(), AppRole.PARENT);

        sessionAuthenticator.establishSession(principal, request, response);
        return ResponseEntity.ok(AuthenticatedUserDto.of(parent, csrfToken(request)));
    }

    /**
     * Issues a CSRF token to a client that has none yet.
     *
     * <p>Sign-in is itself a POST, so without this the very first request of a session
     * would be rejected. Exempting the login endpoints from CSRF instead would permit
     * login CSRF, where an attacker silently signs a victim into an account they control.
     * A GET that mints the token keeps that protection and costs one round trip at startup.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(HttpServletRequest request) {
        String token = csrfToken(request);
        return ResponseEntity.ok(token == null ? Map.of() : Map.of("csrfToken", token));
    }

    /**
     * Rehydrates the SPA on page load and hands it a fresh CSRF token.
     *
     * <p>Permitted anonymously so the client can distinguish "not signed in" (401) from a
     * server fault, rather than assuming a stale localStorage blob is still valid.
     */
    @GetMapping("/session")
    public ResponseEntity<AuthenticatedUserDto> currentSession(HttpServletRequest request) {
        AppUserPrincipal principal = currentPrincipal();
        if (principal == null) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "Sign in to continue.");
        }
        String token = csrfToken(request);

        // Re-read from the database so profile edits and XP changes are reflected, and so a
        // session whose account was deleted is rejected rather than trusted indefinitely.
        if (principal.getRole() == AppRole.STUDENT) {
            StudentEntity student = studentRepository.findById(principal.getId())
                    .orElseThrow(() -> ApiException.unauthorized("SESSION_INVALID",
                            "This account no longer exists. Please sign in again."));
            return ResponseEntity.ok(AuthenticatedUserDto.of(student, token));
        }
        ParentEntity parent = parentRepository.findById(principal.getId())
                .orElseThrow(() -> ApiException.unauthorized("SESSION_INVALID",
                        "This account no longer exists. Please sign in again."));
        return ResponseEntity.ok(AuthenticatedUserDto.of(parent, token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                      HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        sessionAuthenticator.clearSessionCookie(request, response);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Signed out."));
    }

    private static AppUserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    private static String csrfToken(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return token != null ? token.getToken() : null;
    }
}
