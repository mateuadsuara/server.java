package com.github.demonh3x.server.http;

import com.github.demonh3x.server.http.testdoubles.*;
import com.github.demonh3x.server.testdoubles.ConnectionDouble;
import com.github.demonh3x.server.testdoubles.ConnectionFailingToClose;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpTest {
    public static final Response NULL_RESPONSE = new Response(200, "OK", new byte[0]);

    @Test
    public void receivesAGetRequestToRootUri() {
        Request receivedRequest = getInputCommunicationOf("GET / HTTP/1.1\n");

        assertThat(receivedRequest.getMethod(), is("GET"));
        assertThat(receivedRequest.getUri(), is("/"));
    }

    @Test
    public void formatsA200ResponseIntoHttp() {
        String outputCommunication = getOutputCommunicationOf(new Response(200, "OK", "Hello client!".getBytes()));

        assertThat(
                outputCommunication,
                is(
                        "HTTP/1.1 200 OK\n" +
                        "Content-Length: 13\n" +
                        "\n" +
                        "Hello client!"
                )
        );
    }

    @Test
    public void closesTheConnection() {
        RequestHandlerDouble requestHandler = new RequestHandlerDouble(NULL_RESPONSE);
        ConnectionDouble connection = new ConnectionDouble("GET / HTTP/1.1\n");
        new Http(requestHandler).handle(connection);

        assertThat(connection.isClosed(), is(true));
    }

    @Test
    public void doesNotCallTheRequestHandlerAndClosesTheConnectionWhenReceivesAnIncompleteRequest() {
        RequestHandlerSpy requestHandler = new RequestHandlerSpy();
        ConnectionDouble connection = new ConnectionDouble("GET");
        new Http(requestHandler).handle(connection);

        assertThat(requestHandler.hasBeenCalled(), is(false));
        assertThat(connection.isClosed(), is(true));
    }

    @Test
    public void doesNotCallTheRequestHandlerAndClosesTheConnectionWhenThereIsAnExceptionGettingTheInput() {
        RequestHandlerSpy requestHandler = new RequestHandlerSpy();
        ConnectionFailingToGetTheInput connection = new ConnectionFailingToGetTheInput();
        new Http(requestHandler).handle(connection);

        assertThat(requestHandler.hasBeenCalled(), is(false));
        assertThat(connection.isClosed(), is(true));
    }

    @Test
    public void doesNotCallTheRequestHandlerAndClosesTheConnectionWhenThereIsAnExceptionReadingTheInput() {
        RequestHandlerSpy requestHandler = new RequestHandlerSpy();
        ConnectionFailingToReadInput connection = new ConnectionFailingToReadInput();
        new Http(requestHandler).handle(connection);

        assertThat(requestHandler.hasBeenCalled(), is(false));
        assertThat(connection.isClosed(), is(true));
    }

    @Test
    public void triesToCloseTheConnectionWhenThereIsAnExceptionGettingTheOutput() {
        RequestHandlerDouble requestHandler = new RequestHandlerDouble(NULL_RESPONSE);
        ConnectionFailingToWriteOutput connection = new ConnectionFailingToGetTheOutput("GET / HTTP/1.1\n");
        new Http(requestHandler).handle(connection);

        assertThat(connection.isClosed(), is(true));
    }

    @Test
    public void triesToCloseTheConnectionWhenThereIsAnExceptionWritingTheOutput() {
        RequestHandlerDouble requestHandler = new RequestHandlerDouble(NULL_RESPONSE);
        ConnectionFailingToWriteOutput connection = new ConnectionFailingToWriteOutput("GET / HTTP/1.1\n");
        new Http(requestHandler).handle(connection);

        assertThat(connection.isClosed(), is(true));
    }

    @Test
    public void shouldNotBreakWhenThereIsAnExceptionClosingTheConnection() {
        RequestHandlerDouble requestHandler = new RequestHandlerDouble(NULL_RESPONSE);
        new Http(requestHandler).handle(new ConnectionFailingToClose("GET / HTTP/1.1\n"));
    }

    @Test
    public void theExceptionsOccurringInTheRequestHandlerShouldBubbleUpAfterClosingTheConnection() {
        final RuntimeException handlerException = new RuntimeException();
        RequestHandler requestHandler = new RequestHandler() {
            @Override
            public Response handle(Request request) {
                throw handlerException;
            }
        };
        ConnectionDouble connection = new ConnectionDouble("GET / HTTP/1.1\n");

        RuntimeException thrownException = null;
        try {
            new Http(requestHandler).handle(connection);
        } catch (RuntimeException e) {
            thrownException = e;
        }

        assertThat(thrownException, sameInstance(handlerException));
        assertThat(connection.isClosed(), is(true));
    }

    private Request getInputCommunicationOf(String rawInputCommunication) {
        RequestHandlerDouble requestHandler = new RequestHandlerDouble(NULL_RESPONSE);
        new Http(requestHandler).handle(new ConnectionDouble(rawInputCommunication));
        return requestHandler.getReceivedRequest();
    }

    private String getOutputCommunicationOf(Response response) {
        RequestHandlerDouble requestHandler = new RequestHandlerDouble(response);
        ConnectionDouble clientConnection = new ConnectionDouble("GET / HTTP/1.1\n");
        new Http(requestHandler).handle(clientConnection);

        String rawResponse = new String(clientConnection.getOutput());
        return rawResponse;
    }
}
