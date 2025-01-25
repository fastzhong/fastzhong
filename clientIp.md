```java
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientIpUtil {

    /**
     * Retrieves the client's IP address from the request, considering proxy headers if present,
     * and normalizes it to ensure compatibility with both IPv4 and IPv6 formats.
     *
     * @param request the HttpServletRequest object
     * @return the client's IP address in normalized format, or null if invalid
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // If multiple IPs are present, take the first one
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getHeader("Proxy-Client-IP");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr(); // Fallback to remote address
            }
        }

        return normalizeIp(ip);
    }

    /**
     * Normalizes the given IP address to handle IPv4 and IPv6 formats.
     * For example, compresses IPv6 addresses and validates the IP format.
     *
     * @param ip the IP address to normalize
     * @return the normalized IP address, or null if invalid
     */
    private static String normalizeIp(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.getHostAddress(); // Returns compressed IPv6 or standard IPv4
        } catch (UnknownHostException e) {
            // Handle invalid IP address
            return null;
        }
    }
}

```
