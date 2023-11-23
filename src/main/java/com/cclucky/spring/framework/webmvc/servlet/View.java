package com.cclucky.spring.framework.webmvc.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class View {
    private final File viewFile;

    public View(File templateFile) {
        this.viewFile = templateFile;
    }

    public void render(Map<String, ?> model, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder sb = new StringBuilder();

        RandomAccessFile ra = new RandomAccessFile(this.viewFile, "r");

        String line;
        while (null != (line = ra.readLine())) {
            line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

            Pattern pattern = Pattern.compile("￥\\{[^}]+\\}", Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String paramName = matcher.group();
                paramName = paramName.replaceAll("￥\\{|\\}", "");
                Object paramValue = model.get(paramName);
                paramValue = paramValue == null ? "" : paramValue.toString();
                line = matcher.replaceFirst((String) paramValue);
                matcher = pattern.matcher(Objects.requireNonNull(line));
            }
            sb.append(line);
        }
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(sb.toString());
    }
}
