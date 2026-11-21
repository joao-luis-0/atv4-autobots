package com.autobots.automanager.seguranca;

import java.time.Duration;
import java.util.Collections;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import com.autobots.automanager.entidades.Credencial;
import com.autobots.automanager.repositorios.CredencialRepositorio;

@Configuration
public class SegurancaConfiguracao {
    @Bean
    public JwtDecoder jwtDecoder(JwtServico servico) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(servico.chave()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(JwtServico.EMISSOR),
            jwt -> jwt.getExpiresAt() != null && jwt.getSubject() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
        return decoder;
    }
    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, CredencialRepositorio credenciais, JwtServico servico) throws Exception {
        http.csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().formLogin().disable().httpBasic().disable()
            .exceptionHandling()
                .authenticationEntryPoint((req, res, ex) -> erro(res, 401, "Autenticacao necessaria ou token invalido"))
                .accessDeniedHandler((req, res, ex) -> erro(res, 403, "Acesso nao autorizado"))
            .and().authorizeRequests()
                .antMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .antMatchers("/api/usuarios", "/api/usuarios/**", "/api/credencials", "/api/credencials/**",
                             "/api/vendas", "/api/vendas/**", "/api/itemvendas", "/api/itemvendas/**",
                             "/api/servicoprestados", "/api/servicoprestados/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/servicos", "/api/servicos/**", "/api/mercadorias", "/api/mercadorias/**")
                    .hasAnyRole("ADMINISTRADOR", "GERENTE", "VENDEDOR")
                .antMatchers("/api/servicos", "/api/servicos/**", "/api/mercadorias", "/api/mercadorias/**")
                    .hasAnyRole("ADMINISTRADOR", "GERENTE")
                .antMatchers("/cliente/**", "/clientes").hasAnyRole("ADMINISTRADOR", "GERENTE", "VENDEDOR")
                .anyRequest().hasRole("ADMINISTRADOR")
            .and().oauth2ResourceServer()
                .authenticationEntryPoint((req, res, ex) -> erro(res, 401, "Token ausente, invalido ou expirado"))
                .accessDeniedHandler((req, res, ex) -> erro(res, 403, "Acesso nao autorizado"))
                .jwt().jwtAuthenticationConverter(jwt -> {
                    try {
                        Credencial credencial = credenciais.findById(Long.valueOf(jwt.getSubject())).orElseThrow();
                        if (!servico.versao(credencial).equals(jwt.getClaimAsString("versao")) || credencial.getUsuario().getPerfil() == null)
                            throw new IllegalArgumentException();
                        return new UsernamePasswordAuthenticationToken(credencial.getUsuario(), null,
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_" + credencial.getUsuario().getPerfil().name())));
                    } catch (RuntimeException ex) {
                        throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
                    }
                });
        return http.build();
    }
    private static void erro(javax.servlet.http.HttpServletResponse res, int status, String mensagem) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
