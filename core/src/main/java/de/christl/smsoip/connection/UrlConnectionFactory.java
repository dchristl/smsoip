/*
 * Copyright (c) Danny Christl 2012.
 *     This file is part of SMSoIP.
 *
 *     SMSoIP is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SMSoIP is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.connection;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Factory and Helper for everything with URLConnection
 */
public class UrlConnectionFactory {


    private String url;
    private String method;

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    private static final String CRLF = "\r\n";
    /**
     * the prefered targetAgent
     */
    private String targetAgent = "Mozilla/3.0 (compatible)";
    /**
     * TIMEOUT can be used for any connection
     */
    private int timeout = 10000;
    private List<String> cookies;
    private Map<String, String> requestProperties;
    private HttpURLConnection con;
    private boolean followRedirects = true;

    public UrlConnectionFactory(String url) {
        this(url, METHOD_POST);
    }

    public UrlConnectionFactory(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public HttpURLConnection create() throws IOException {
        con = (HttpURLConnection) new URL(url).openConnection();
        con.setReadTimeout(timeout);
        con.setConnectTimeout(timeout);
        if (targetAgent != null) {
            con.setRequestProperty("User-Agent", targetAgent);
        }
        con.setRequestMethod(method);
        con.setInstanceFollowRedirects(followRedirects);
        if (cookies != null) {
            StringBuilder cookieBuilder = new StringBuilder();
            for (int i = 0, sessionCookiesSize = cookies.size(); i < sessionCookiesSize; i++) {
                String sessionCookie = cookies.get(i);
                cookieBuilder.append(sessionCookie).append(i + 1 < sessionCookiesSize ? "; " : "");
            }
            con.setRequestProperty("Cookie", cookieBuilder.toString());
        }
        if (requestProperties != null) {
            for (Map.Entry<String, String> stringStringEntry : requestProperties.entrySet()) {
                con.setRequestProperty(stringStringEntry.getKey(), stringStringEntry.getValue());
            }

        }
        return con;
    }


    public void setTargetAgent(String targetAgent) {
        this.targetAgent = targetAgent;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * makes and inputstring to a readable String
     * <b>Use this only for debugging in productive env. this will be really slow and normally not needed</b>
     *
     * @param is the inputstream for exchanging
     * @return UTF-8 encoded String
     * @throws IOException
     */
    public static String inputStream2DebugString(InputStream is) throws IOException {
        return inputStream2DebugString(is, "UTF-8");
    }

    /**
     * makes and inputstring to a readable String
     * <b>Use this only for debugging in productive env. this will be really slow and normally not needed</b>
     *
     * @param is       the inputstream for exchanging
     * @param encoding the encoding to use
     * @return String with preferred encoding
     * @throws IOException
     */
    public static String inputStream2DebugString(InputStream is, String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
        String line;
        StringBuilder returnFromServer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            returnFromServer.append(line);
        }
        return returnFromServer.toString();
    }

    /**
     * find the cookie by given name, the cookie must use uppercase letters
     *
     * @param headerFields
     * @param cookieName
     * @return
     */
    public static String findCookieByName(Map<String, List<String>> headerFields, String cookieName) {
        for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
            String cookieList = stringListEntry.getKey();
            if (cookieList != null && cookieList.equalsIgnoreCase("set-cookie")) {
                for (String cookie : stringListEntry.getValue()) {
                    if (cookie != null && cookie.toUpperCase().startsWith(cookieName.toUpperCase() + "=")) {
                        return cookie;
                    }
                }
            }
        }
        return null; //not found
    }

    public static String findCookieByPattern(Map<String, List<String>> headerFields, String pattern) {
        for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
            String cookieList = stringListEntry.getKey();
            if (cookieList != null && cookieList.equalsIgnoreCase("set-cookie")) {
                for (String cookie : stringListEntry.getValue()) {
                    if (cookie != null && cookie.matches(pattern)) {
                        return cookie;
                    }
                }
            }
        }
        return null; //not found
    }

    public static List<String> findCookiesByPattern(Map<String, List<String>> headerFields, String pattern) {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
            String cookieList = stringListEntry.getKey();
            if (cookieList != null && cookieList.equalsIgnoreCase("set-cookie")) {
                for (String cookie : stringListEntry.getValue()) {
                    if (cookie != null && cookie.matches(pattern)) {
                        out.add(cookie);
                    }
                }
            }
        }
        return out;
    }


    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

    public void setRequestProperties(Map<String, String> requestProperties) {
        this.requestProperties = requestProperties;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public HttpURLConnection writeBody(String body) throws IOException {
        if (con == null) {
            create();
        }
        con.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
        writer.write(body);
        writer.flush();
        writer.close();
        return con;
    }

    public HttpURLConnection getConnnection() throws IOException {
        if (con == null) {
            create();
        }
        return con;
    }

    public void writeMultipartBody(Map<String, String> parameterMap, String encoding) throws IOException {
        if (con == null) {
            create();
        }
        con.setDoOutput(true);
        String boundary = "--" + Long.toHexString(System.currentTimeMillis());
        OutputStream output = con.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, encoding), true);
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        for (Map.Entry<String, String> stringStringEntry : parameterMap.entrySet()) {
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"").append(stringStringEntry.getKey()).append("\"").append(CRLF);
            writer.append(CRLF);
            writer.append(stringStringEntry.getValue()).append(CRLF).flush();

        }
        writer.append("--").append(boundary).append("--").append(CRLF).flush();
        writer.close();
    }

}
