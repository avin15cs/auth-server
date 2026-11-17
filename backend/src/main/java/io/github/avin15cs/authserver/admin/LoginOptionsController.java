package io.github.avin15cs.authserver.admin;

import io.github.avin15cs.authserver.security.PasswordLoginAvailability;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login-options")
public class LoginOptionsController {

    private final PasswordLoginAvailability passwordLogin;

    public LoginOptionsController(PasswordLoginAvailability passwordLogin) {
        this.passwordLogin = passwordLogin;
    }

    @GetMapping
    public LoginOptions loginOptions(CsrfToken csrfToken) {
        return new LoginOptions(
                passwordLogin.enabled(),
                new CsrfView(csrfToken.getParameterName(), csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    public record LoginOptions(boolean passwordLoginEnabled, CsrfView csrf) {
    }

    public record CsrfView(String parameterName, String headerName, String token) {
    }
}
