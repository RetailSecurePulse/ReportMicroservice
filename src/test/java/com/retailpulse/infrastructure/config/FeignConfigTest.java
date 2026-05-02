package com.retailpulse.infrastructure.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignConfigTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void feignLoggerLevel_returnsFull() {
        FeignConfig feignConfig = new FeignConfig(tracerProvider(tracer));

        assertEquals(Logger.Level.FULL, feignConfig.feignLoggerLevel());
    }

    @Test
    void interceptor_withJwtAuthenticationAndTrace_addsAuthorizationAndTracingHeaders() {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-id");
        when(traceContext.spanId()).thenReturn("span-id");

        Jwt jwt = Jwt.withTokenValue("jwt-token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        RequestTemplate template = requestTemplate();
        RequestInterceptor interceptor = new FeignConfig(tracerProvider(tracer)).oauth2BearerForwardingInterceptor();

        interceptor.apply(template);

        assertEquals(List.of("trace-id"), List.copyOf(template.headers().get("X-B3-TraceId")));
        assertEquals(List.of("span-id"), List.copyOf(template.headers().get("X-B3-SpanId")));
        assertEquals(List.of("Bearer jwt-token"), List.copyOf(template.headers().get(HttpHeaders.AUTHORIZATION)));
    }

    @Test
    void interceptor_withBearerTokenAuthentication_addsAuthorizationHeader() {
        when(tracer.currentSpan()).thenReturn(null);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "bearer-token",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );
        DefaultOAuth2AuthenticatedPrincipal principal =
                new DefaultOAuth2AuthenticatedPrincipal(Map.of("sub", "user-1"), AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(
                new BearerTokenAuthentication(principal, accessToken, AuthorityUtils.NO_AUTHORITIES)
        );

        RequestTemplate template = requestTemplate();
        RequestInterceptor interceptor = new FeignConfig(tracerProvider(tracer)).oauth2BearerForwardingInterceptor();

        interceptor.apply(template);

        assertEquals(List.of("Bearer bearer-token"), List.copyOf(template.headers().get(HttpHeaders.AUTHORIZATION)));
        assertFalse(template.headers().containsKey("X-B3-TraceId"));
    }

    @Test
    void interceptor_withoutSecurityToken_usesIncomingRequestAuthorizationHeader() {
        when(tracer.currentSpan()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer forwarded-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = requestTemplate();
        RequestInterceptor interceptor = new FeignConfig(tracerProvider(tracer)).oauth2BearerForwardingInterceptor();

        interceptor.apply(template);

        assertEquals(List.of("Bearer forwarded-token"), List.copyOf(template.headers().get(HttpHeaders.AUTHORIZATION)));
    }

    @Test
    void interceptor_withoutAnyToken_doesNotSetAuthorizationHeader() {
        when(tracer.currentSpan()).thenReturn(null);

        RequestTemplate template = requestTemplate();
        RequestInterceptor interceptor = new FeignConfig(tracerProvider(tracer)).oauth2BearerForwardingInterceptor();

        interceptor.apply(template);

        assertFalse(template.headers().containsKey(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void interceptor_withNonBearerIncomingAuthorizationHeader_doesNotSetAuthorizationHeader() {
        when(tracer.currentSpan()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = requestTemplate();
        RequestInterceptor interceptor = new FeignConfig(tracerProvider(tracer)).oauth2BearerForwardingInterceptor();

        interceptor.apply(template);

        assertFalse(template.headers().containsKey(HttpHeaders.AUTHORIZATION));
    }

    private RequestTemplate requestTemplate() {
        RequestTemplate template = new RequestTemplate();
        template.method(Request.HttpMethod.GET);
        template.target("http://localhost:8080");
        template.uri("/api/products");
        return template;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<Tracer> tracerProvider(Tracer tracer) {
        ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        return tracerProvider;
    }
}
