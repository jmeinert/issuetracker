package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.security.UserPrincipal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private UserPrincipal principal;

    private JwtEncoder jwtEncoder;

    private JwtDecoder jwtDecoder;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        SecretKey jwtSecretKey = new SecretKeySpec(new byte[32], "HmacSHA256");

        jwtEncoder = NimbusJwtEncoder.withSecretKey(jwtSecretKey)
            .algorithm(MacAlgorithm.HS256)
            .build();

        jwtDecoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        tokenService = new TokenService(jwtEncoder);
    }

    @Test
    void generateToken_createsExpectedClaimsAndReturnsEncodedToken() {
        Long userId = 1L;

        when(principal.getId())
            .thenReturn(userId);

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)
            )
        );

        String token = tokenService.generateToken(authentication);
        Jwt jwt = jwtDecoder.decode(token);

        Instant issuedAt = Objects.requireNonNull(jwt.getIssuedAt(), "iat claim must be present");
        Instant expiresAt = Objects.requireNonNull(jwt.getExpiresAt(), "exp claim must be present");

        assertThat(jwt.getClaimAsString(JwtClaimNames.ISS)).isEqualTo(TokenService.TOKEN_ISSUER);
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(Duration.between(issuedAt, expiresAt)).isEqualTo(Duration.ofMinutes(15L));
        assertThat(jwt.getClaimAsStringList(TokenService.ROLES_CLAIM)).containsExactly("USER");
    }
}
