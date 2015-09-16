package org.apache.zeppelin.socket;

import org.apache.zeppelin.server.ZeppelinServer;
import org.apache.zeppelin.utils.SecurityContextHolder;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;

/**
 * Created by piyush.mukati on 14/09/15.
 */
public class WebSocketAuth {

private final String authFilterClassName;
    public WebSocketAuth(String authFilterClassName) {
        this.authFilterClassName=authFilterClassName;
    }

    /**
     *
     * for in valid access subject value will be null.
     * @param req
     * @return
     */
    public String validate(HttpServletRequest req) {

        try {
            Class cls = ZeppelinServer.class.getClassLoader().loadClass(authFilterClassName);
            Method doFilter = cls.getMethod("doFilter", ServletRequest.class, ServletResponse.class, FilterChain.class);
            doFilter.invoke(cls.newInstance(), req, null, dummyFilterChain);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return SecurityContextHolder.getSecurityContext().getUser();
    }

    /**
     * for in valid access subject value will be null.
     * @param token
     * @return
     */
    private final FilterChain dummyFilterChain = new FilterChain() {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
            return;
        }
    };



}
