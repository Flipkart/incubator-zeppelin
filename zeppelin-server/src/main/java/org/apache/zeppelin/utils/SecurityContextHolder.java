package org.apache.zeppelin.utils;

/**
 * Created by piyush.mukati on 14/09/15.
 */
public class SecurityContextHolder {
    private static ThreadLocal<SecurityContext> securityContext = new ThreadLocal<SecurityContext>();

    public static SecurityContext getSecurityContext() {
        if(securityContext.get() == null) {
            securityContext.set(new SecurityContext());
        }

        return securityContext.get();
    }

    public static void setSecurityContext(SecurityContext securityContext) {
        SecurityContextHolder.securityContext.set(securityContext);
    }

    public static void clearSecurityContext() {
        securityContext.remove();
    }
}