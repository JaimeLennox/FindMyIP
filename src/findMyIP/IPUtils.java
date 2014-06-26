package findMyIP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class IPUtils {

    private static final URL IP_CHECK_URL =
            makeURL("http://ipchk.sourceforge.net/rawip/");

    private static URL makeURL(String URLString) {

        try {
            return new URL(URLString);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getIPAddress() {
        String IPAddress = getIPFromURL();

        // Fallback.
        if (IPAddress == null) {
            IPAddress = getIPFromAPI();
        }

        if (IPAddress == null) {
            return "<unknown>";
        }

        return IPAddress;
    }

    private static String getIPFromURL() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                IP_CHECK_URL.openStream()));) {
            String ipString = in.readLine();
            if (validIP(ipString)) {
                return ipString;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getIPFromAPI() {
        try {
            InetAddress IP = InetAddress.getLocalHost();
            return IP.getHostAddress();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean validIP(String IPAddress) {
        return IPAddress.matches("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
    }
}
