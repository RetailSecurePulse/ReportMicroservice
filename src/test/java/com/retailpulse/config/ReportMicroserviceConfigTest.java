package com.retailpulse.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReportMicroserviceConfigTest {

    @Test
    void corsConfigurationSource_usesConfiguredOriginHeadersAndMethods() {
        ReportMicroserviceConfig config = new ReportMicroserviceConfig();
        ReflectionTestUtils.setField(config, "originURL", "http://frontend.example");

        CorsConfigurationSource source =
                ReflectionTestUtils.invokeMethod(config, "corsConfigurationSource");
        assert source != null;
        CorsConfiguration corsConfiguration = source.getCorsConfiguration(new MockHttpServletRequest());

        assert corsConfiguration != null;
        assertEquals(List.of("http://frontend.example"), corsConfiguration.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"), corsConfiguration.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type"), corsConfiguration.getAllowedHeaders());
        assertEquals(List.of("Authorization"), corsConfiguration.getExposedHeaders());
        assertEquals(Boolean.TRUE, corsConfiguration.getAllowCredentials());
    }

    @Test
    void jwtGrantedAuthoritiesConverter_readsRolesClaimWithRolePrefix() {
        ReportMicroserviceConfig config = new ReportMicroserviceConfig();
        JwtGrantedAuthoritiesConverter converter =
                ReflectionTestUtils.invokeMethod(config, "jwtGrantedAuthoritiesConverter");

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("roles", List.of("ADMIN", "MANAGER"))
                .build();

        assert converter != null;
        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertEquals(List.of("ROLE_ADMIN", "ROLE_MANAGER"), authorities);
    }

    @Test
    void jwtAuthenticationConverter_usesConfiguredAuthoritiesConverter() {
        ReportMicroserviceConfig config = new ReportMicroserviceConfig();
        JwtAuthenticationConverter converter =
                ReflectionTestUtils.invokeMethod(config, "jwtAuthenticationConverter");

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("roles", List.of("INVENTORY_MANAGER"))
                .build();

        assert converter != null;
        List<String> authorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertEquals(List.of("ROLE_INVENTORY_MANAGER", "FACTOR_BEARER"), authorities);
    }

    @Test
    void securityFilterChain_buildsWhenAuthDisabled() throws Exception {
        ReportMicroserviceConfig config = new ReportMicroserviceConfig();
        ReflectionTestUtils.setField(config, "authEnabled", false);
        ReflectionTestUtils.setField(config, "originURL", "http://frontend.example");

        SecurityFilterChain chain = config.securityFilterChain(httpSecurity());

        assertNotNull(chain);
    }

    @Test
    void securityFilterChain_buildsWhenAuthEnabled() throws Exception {
        ReportMicroserviceConfig config = new ReportMicroserviceConfig();
        ReflectionTestUtils.setField(config, "authEnabled", true);
        ReflectionTestUtils.setField(config, "originURL", "http://frontend.example");
        ReflectionTestUtils.setField(config, "keySetUri", "http://localhost/jwks");

        SecurityFilterChain chain = config.securityFilterChain(httpSecurity());

        assertNotNull(chain);
    }

    private HttpSecurity httpSecurity() throws Exception {
        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };

        AuthenticationManagerBuilder authenticationManagerBuilder =
                new AuthenticationManagerBuilder(objectPostProcessor);

        HttpSecurity httpSecurity = new HttpSecurity(objectPostProcessor, authenticationManagerBuilder, new HashMap<>());
        ApplicationContext applicationContext = new StaticApplicationContext();
        httpSecurity.setSharedObject(ApplicationContext.class, applicationContext);
        return httpSecurity;
    }
}
