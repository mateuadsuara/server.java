package com.github.demonh3x.server.http.request;

import com.github.demonh3x.server.http.Request;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RequestParserTest {
    @Test
    public void receivesAGetRequestToRootUri() throws IOException {
        Request receivedRequest = readFrom(
                "GET / HTTP/1.1\r\n" +
                "\r\n"
        );
        assertThat(receivedRequest.getMethod(), is("GET"));
        assertThat(receivedRequest.getUri(), is("/"));
    }

    @Test
    public void receivesAPostRequestToHomeUri() throws IOException {
        Request receivedRequest = readFrom(
                "POST /home HTTP/1.1\r\n" +
                "\r\n"
        );
        assertThat(receivedRequest.getMethod(), is("POST"));
        assertThat(receivedRequest.getUri(), is("/home"));
    }

    @Test
    public void receivesHeaders() throws IOException {
        Request receivedRequest = readFrom(
                "GET / HTTP/1.1\r\n" +
                "HeaderName1: header:Value1\r\n" +
                "HeaderName2: header:Value2\r\n" +
                "\r\n"
        );
        assertThat(receivedRequest.getHeaders(), CoreMatchers.<Map<String, String>>is(new HashMap<String, String>() {{
            put("HeaderName1", "header:Value1");
            put("HeaderName2", "header:Value2");
        }}));
    }

    @Test
    public void receivesAPostRequestWithAnEntity() throws IOException {
        Request receivedRequest = readFrom(
                "POST /home HTTP/1.1\r\n" +
                "Content-Length: 5\r\n" +
                "\r\n" +
                "abcde"
        );
        assertThat(receivedRequest.getMessageBody(), is("abcde".getBytes()));
    }

    @Test
    public void receivesARequestWithAnEntityButNoContentLength() throws IOException {
        Request receivedRequest = readFrom(
                "POST /home HTTP/1.1\r\n" +
                "\r\n" +
                "abcde"
        );
        assertThat(receivedRequest.getMessageBody(), is(new byte[0]));
    }

    @Test
    public void receivesARequestWithContentLengthSmallerThanTheEntity() throws IOException {
        Request receivedRequest = readFrom(
                "POST /home HTTP/1.1\r\n" +
                "Content-Length: 5\r\n" +
                "\r\n" +
                "123456789"
        );
        assertThat(receivedRequest.getMessageBody(), is("12345".getBytes()));
    }

    @Test (expected = IOException.class)
    public void receivesARequestWithContentLengthGreaterThanTheEntity() throws IOException {
        readFrom(
                "POST /home HTTP/1.1\r\n" +
                "Content-Length: 10\r\n" +
                "\r\n" +
                "12345"
        );
    }

    private Request readFrom(String rawRequest) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes());
        return new RequestParser().read(inputStream);
    }
}