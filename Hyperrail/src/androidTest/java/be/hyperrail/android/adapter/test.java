package be.hyperrail.android.adapter;

import android.util.Base64;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class test {
    @Test
    public void test() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        TreeMap<String, Object> params = new TreeMap<String, Object>();

        params.put("apiKey", "4_JZvZObbVWc1YROHF9e6y8A");
        params.put("fields", "country");
        params.put("format", "json");
        params.put("gmid", "gmid.ver4.AtLtysxd1A.tuO6_j3dzVgeMSdJMTO-iPpB4Z10oxiQ7m5HCkRtWapV1ROv_eRC6NXD03hjNzeh.t4SJ2b_AWCTOLvSBH7eicvfc4BZiPWQUm7emMc8azPfmn7P7k2UpsqwNX5GOZKbuemy3mf_pTpNrjunSwabhaw.sc3");
        params.put("httpStatusCodes", "false");
        params.put("nonce", "1732452845489_756970290");
        params.put("oauth_token", "st2.s.AtLtpm6QEA.Il-waYA8JyhEaNnuxNwTRdJVBFCIg83O8iJtkxZQwaBbarXPj6ceDayDS7nfYeI-JQ21jrNFkm2SGuja_2KItti_Mliw8dne0uUwsXQJREa-LDMxbwwPe0csqRztFwUq.jdXhcgLvS1YdcQjiuDKQxF8BSAcvjAUbLC57LFxGZQ4wLVmj8QCgi4pUknFlShTuET2EsbDBhLzioroog-wDfA.sc3");
        params.put("sdk", "Android_7.0.11");
        params.put("targetEnv", "mobile");
        params.put("timestamp", "1732452840");
        params.put("ucid", "Ab3kjdtmRrj6g572fT4RIg");


        String sig = getSignature("VgHTFHNzCEEbkjw/AWNmx2fOKHs=", "POST", "https://accounts.eu1.gigya.com/accounts.getJWT", params);
        System.err.println("Signature " + sig);
    }

    @Test
    public void createSig() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        TreeMap<String, Object> params = new TreeMap<String, Object>();

        params.put("apiKey", "4_JZvZObbVWc1YROHF9e6y8A");
        params.put("fields", "country");
        params.put("format", "json");
        params.put("gmid", "gmid.ver4.AtLtysxd1A.tuO6_j3dzVgeMSdJMTO-iPpB4Z10oxiQ7m5HCkRtWapV1ROv_eRC6NXD03hjNzeh.t4SJ2b_AWCTOLvSBH7eicvfc4BZiPWQUm7emMc8azPfmn7P7k2UpsqwNX5GOZKbuemy3mf_pTpNrjunSwabhaw.sc3");
        params.put("httpStatusCodes", "false");
        params.put("nonce", "1732478189897_-590969371");
        params.put("oauth_token", "st2.s.AtLt7qG2zg.7smyFjMxfUMFU4EJCK7TCHn-A0qKvuzIWEBfQqF9xWGc6xQ57n5kQQXN6cxriRhBjdLP7SPndLlkO7o4uBjHa9pg4nZstHLjgnjhTFwAocFNVNvwUUXbTtJRwkcvHa6y.NAA4HclBD2vCFNnF3i8qKTSLqljD3_uRdNYk8l0zaZrprMFar-YaxA5pG_i87rnbry73bj8QFKOXeRP0WVfELg.sc3");
        params.put("sdk", "Android_7.0.11");
        params.put("targetEnv", "mobile");
        params.put("timestamp", "1732478189");
        params.put("ucid", "Ab3kjdtmRrj6g572fT4RIg");


        String sig = getSignature("VxgSXUFyXo1vz19xpbjpFb7G3Bg=", "POST", "https://accounts.eu1.gigya.com/accounts.getJWT", params);
        System.err.println("Signature " + sig);
    }

    public static String getSignature(String secretKey, String method, String urlString, TreeMap<String, Object> params) {
        String ampersand = "&";
        String result = null;

        // Validate the input parameters
        if (params == null || urlString == null || method == null || secretKey == null) {
            return result;  // Return null if any parameter is invalid
        }

        try {
            // Build URL details
            StringBuilder urlBuilder = new StringBuilder();
            URL url = new URL(urlString);
            String protocol = url.getProtocol().toLowerCase(Locale.ENGLISH);
            urlBuilder.append(protocol).append("://");
            String host = url.getHost().toLowerCase(Locale.ENGLISH);
            urlBuilder.append(host);

            // Check if we need to append the port based on protocol (HTTP or HTTPS)
            String protocolUpper = url.getProtocol().toUpperCase(Locale.ENGLISH);
            if ("HTTP".equals(protocolUpper)) {
                int port = url.getPort();
                if (port != 80 && port != -1) {
                    urlBuilder.append(":").append(port);
                }
            } else if ("HTTPS".equals(protocolUpper)) {
                int port = url.getPort();
                if (port != 443 && port != -1) {
                    urlBuilder.append(":").append(port);
                }
            }

            // Append the path
            urlBuilder.append(url.getPath());

            // Append method and parameters
            StringBuilder signatureBuilder = new StringBuilder();
            signatureBuilder.append(method.toUpperCase(Locale.ENGLISH)).append(ampersand);
            String encodedUrl = urlEncode(urlBuilder.toString());
            signatureBuilder.append(encodedUrl).append(ampersand);

            // Encode query parameters
            String encodedParams = buildEncodedQuery(params);
            String encodedParamsStr = urlEncode(encodedParams);
            signatureBuilder.append(encodedParamsStr);

            // Concatenate the signature
            String signatureData = signatureBuilder.toString();
            System.err.println("ENCODING " + signatureData);
            // Encode the signature with the secret key
            result = encodeSignature(signatureData, secretKey);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;  // Return the generated signature or null in case of error
    }

    private static String encodeSignature(String data, String secretKey) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] decode = Base64.decode(secretKey, 0);
        byte[] bytes = data.getBytes("UTF-8");
        SecretKeySpec secretKeySpec = new SecretKeySpec(decode, "HmacSHA1");
        Mac instance = Mac.getInstance("HmacSHA1");
        instance.init(secretKeySpec);
        String result = Base64.encodeToString(instance.doFinal(bytes), 10);
        return result;
    }

    public static String buildEncodedQuery(Map<String, Object> map) {
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder sb2 = new StringBuilder();
        for (Map.Entry next : map.entrySet()) {
            Object value = next.getValue();
            String str = (String) next.getKey();
            if (value != null) {
                sb2.append(str);
                sb2.append('=');
                sb2.append(urlEncode(String.valueOf(next.getValue())));
                sb2.append('&');
            }
        }
        if (sb2.length() > 0) {
            sb2.deleteCharAt(sb2.length() - 1);
        }
        return sb2.toString();
    }

    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (Exception unused) {
            return null;
        }
    }


    public static String getBaseUrl(String str, String apiDomain) {
        StringBuilder sb2 = new StringBuilder();
        String[] split = str.split("\\.");

        sb2.append("https://");
        sb2.append(split[0]);
        sb2.append(".");
        sb2.append(apiDomain);
        sb2.append("/");
        sb2.append(str);
        System.err.println("Base url " + sb2.toString());
        return sb2.toString();
    }
}
