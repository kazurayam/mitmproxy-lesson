package io.github.bonigarcia.webdriver.jupiter.ch09.network_traffic;

import io.appium.mitmproxy.InterceptedMessage;

import java.util.List;

public class InterceptedMessageFormatter {

    public static String format(InterceptedMessage im) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        if (im.getRequest() != null) {
            InterceptedMessage.Request req = im.getRequest();
            sb.append("\"request\": {");
            sb.append(String.format("\"method\": \"%s\", ", req.getMethod()));
            sb.append(String.format("\"url\": \"%s\"", req.getUrl()));
            if (req.getHeaders() != null) {
                sb.append(", ");
                sb.append("\"headers\": ");
                sb.append(stringifyHeaders(req.getHeaders()));
            }
            sb.append("}");
        }
        sb.append(",\n");
        if (im.getResponse() != null) {
            InterceptedMessage.Response res = im.getResponse();
            sb.append("\"response\": {");
            sb.append(String.format("\"status\": %d", res.getStatusCode()));
            if (res.getHeaders() != null) {
                sb.append(", ");
                sb.append("\"headers\": ");
                sb.append(stringifyHeaders(res.getHeaders()));
            }
            sb.append("}");
        }
        sb.append("\n}");
        return sb.toString();
    }

    private static String stringifyHeaders(List<String[]> headers) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int hdCount = 0;
        for (String[] hds : headers) {
            if (hdCount > 0) {
                sb.append(", ");
            }
            sb.append("[");
            int itemCount = 0;
            for (String hd : hds) {
                if (itemCount > 0) {
                    sb.append(", ");
                }
                sb.append(String.format("\"%s\"", hd));
                itemCount += 1;
            }
            sb.append("]");
            hdCount += 1;
        }
        sb.append("]");
        return sb.toString();
    }
}