package dev.flyfish.boot.cas.validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class SecureURL {
    private static final Log log = LogFactory.getLog(SecureURL.class);

    public SecureURL() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        System.out.println(retrieve(args[0]));
    }

    public static String retrieve(String url) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("entering retrieve(" + url + ")");
        }

        URL u = URI.create(url).toURL();
        URLConnection uc = u.openConnection();
        uc.setRequestProperty("Connection", "close");
        InputStream in = uc.getInputStream();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (int chByte = in.read(); chByte != -1; chByte = in.read()) {
            output.write(chByte);
        }

        return output.toString(StandardCharsets.UTF_8);
    }
}
