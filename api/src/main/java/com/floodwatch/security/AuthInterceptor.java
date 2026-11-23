/*
 * File:    AuthInterceptor.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks the login token and role on endpoints marked with RequiresRole.
 */


package com.floodwatch.security;

import com.floodwatch.model.Role;
import com.floodwatch.model.User;
import com.floodwatch.service.AuthService;
import com.floodwatch.service.ForbiddenException;
import com.floodwatch.service.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * Runs before each controller method. Endpoints without RequiresRole pass straight through. For the
 * others, a missing, malformed, unknown or expired token gives 401, and a valid token whose role is not
 * allowed gives 403. A valid user is stored on the request for CurrentUser.of(request).
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class AuthInterceptor implements HandlerInterceptor {


    private static final String MISSING_LOGIN = "Missing, invalid or expired login token.";
    private static final String WRONG_ROLE = "Your role is not allowed to use this.";

    private final AuthService auth;

    /**
     * Creates the interceptor.
     *
     * @param auth the service that resolves tokens to users
     */


    public AuthInterceptor
    (
        AuthService auth
    ) {
        this.auth = auth;
    }

    /**
     * Checks the caller before a controller method runs.
     *
     * @param request  the incoming request
     * @param response the outgoing response
     * @param handler  the controller method about to run
     * @return true to let the request continue
     * @throws UnauthorizedException when the token is missing, malformed, unknown or expired
     * @throws ForbiddenException    when the token is valid but its role is not allowed
     */


    @Override
    public boolean preHandle
    (
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) {
        if (!(handler instanceof HandlerMethod method))
        {
            return true;
        }
        Optional<RequiresRole> rule = findRule(method);
        if (rule.isEmpty())
        {
            return true;
        }
        Optional<String> token = BearerToken.parse(request.getHeader(HttpHeaders.AUTHORIZATION));
        User user = token.flatMap(auth::userForToken)
                .orElseThrow(() -> new UnauthorizedException(MISSING_LOGIN));
        List<Role> allowed = List.of(rule.get().value());
        if (!allowed.isEmpty() && !allowed.contains(user.role()))
        {
            throw new ForbiddenException(WRONG_ROLE);
        }
        CurrentUser.store(request, user);
        return true;
    }

    /**
     * Finds the RequiresRole rule for a method. A rule on the method replaces a rule on its class.
     *
     * @param method the controller method about to run
     * @return the rule, or empty when the endpoint is public
     */


    private static Optional<RequiresRole> findRule
    (
        HandlerMethod method
    ) {
        return Optional.ofNullable(method.getMethodAnnotation(RequiresRole.class))
                .or(() -> Optional.ofNullable(AnnotationUtils.findAnnotation(method.getBeanType(), RequiresRole.class)));
    }
}