package org.apache.zeppelin.utils;

import org.apache.zeppelin.server.ZeppelinServer;

import javax.servlet.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import javax.ws.rs.core.SecurityContext;


/**
 * Created by piyush.mukati on 14/09/15.
 */
public class AuthFilter implements Filter {



    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("filter  user=yoyooy");
        SecurityContextHolder.getSecurityContext().setUser("yoyooy");

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
