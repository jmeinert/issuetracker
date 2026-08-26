package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.security.UserPrincipal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
public class TokenService {

    public static final String TOKEN_ISSUER = "issuetracker";
    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;

    public TokenService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        String subject = determineSubject(authentication);
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(Objects::nonNull)
            .filter(authority -> authority.startsWith(UserPrincipal.ROLE_PREFIX))
            .map(authority -> authority.substring(UserPrincipal.ROLE_PREFIX.length()))
            .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(TOKEN_ISSUER)
            .issuedAt(now)
            .expiresAt(now.plus(15, ChronoUnit.MINUTES))
            .subject(subject)
            .claim(ROLES_CLAIM, roles)
            .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String determineSubject(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new IllegalArgumentException("Expected UserPrincipal for token generation");
        }

        return userPrincipal.getId().toString();
    }
}
