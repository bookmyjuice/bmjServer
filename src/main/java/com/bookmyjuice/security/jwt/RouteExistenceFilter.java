package com.bookmyjuice.security.jwt;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * B-09 FIX: Route existence check filter.
 * 
 * Before Spring Security applies authentication, this filter checks if the
 * requested route actually exists in the application by iterating through
 * all registered HandlerMapping beans.
 * 
 * If the route doesn't exist (no handler mapping found), the request bypasses
 * the security filter chain entirely and proceeds directly to the
 * DispatcherServlet, which will return a proper 404 response.
 * 
 * This prevents unknown API routes (e.g., /api/v1/auth/signin) from returning
 * confusing 401 "Unauthorized" errors when they should return 404 "Not Found".
 */
@Component
public class RouteExistenceFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RouteExistenceFilter.class);

    @Autowired
    private List<HandlerMapping> handlerMappings;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply to /api/ routes (non-API routes are already .anyRequest().permitAll())
        if (path.startsWith("/api/")) {
            boolean routeExists = false;
            try {
                // Check through all registered HandlerMapping beans to see if any
                // can handle this request
                for (HandlerMapping mapping : handlerMappings) {
                    HandlerExecutionChain handler = mapping.getHandler(request);
                    if (handler != null) {
                        routeExists = true;
                        break;
                    }
                }
            } catch (Exception e) {
                // If handler lookup fails for any reason, log and continue
                // with normal security chain
                logger.trace("B-09: Handler lookup error for [{} {}]: {}", request.getMethod(), path, e.getMessage());
            }

            if (!routeExists) {
                // No handler found for this API route - write 404 directly
                // and do NOT pass through the security chain (which would return 401)
                logger.debug("B-09: No handler for [{} {}], returning 404", 
                        request.getMethod(), path);
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setContentType("application/json");
                response.getWriter().write(
                    String.format("{\"path\":\"%s\",\"error\":\"Not Found\",\"message\":\"No handler found for this route\",\"status\":404}", 
                        path));
                response.getWriter().flush();
                return;
            }
        }

        // Route exists (or non-API route) - continue with normal security chain
        chain.doFilter(request, response);
    }
}
