package de.otto.edison.hystrix.http;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.testng.annotations.Test;

import java.io.IOException;

import static de.otto.edison.hystrix.http.HttpCommandBuilder.httpCommand;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class HttpCommandTest {

    enum TestGroup implements HystrixCommandGroupKey {
        TEST
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldExecuteRequest() throws IOException {
        // given
        AsyncHttpClient.BoundRequestBuilder mockRequest = mock(AsyncHttpClient.BoundRequestBuilder.class);
        when(mockRequest.execute()).thenReturn(mock(ListenableFuture.class));

        // when
        httpCommand()
                .inGroup(TestGroup.TEST)
                .forRequest(mockRequest)
                .build()
                .execute();
        // then
        verify(mockRequest, atLeastOnce()).execute();
    }

    @Test(expectedExceptions = HystrixRuntimeException.class)
    @SuppressWarnings("unchecked")
    public void shouldThrowExceptionWhenExecutionFails() throws IOException {
        // given
        AsyncHttpClient.BoundRequestBuilder mockRequest = mock(AsyncHttpClient.BoundRequestBuilder.class);
        when(mockRequest.execute()).thenThrow(new IOException());

        // when
        httpCommand()
                .inGroup(TestGroup.TEST)
                .forRequest(mockRequest)
                .build()
                .execute();
        // then an IOException is thrown.
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFallbackWhenExecutionFails() throws IOException {
        // given
        AsyncHttpClient.BoundRequestBuilder mockRequest = mock(AsyncHttpClient.BoundRequestBuilder.class);
        when(mockRequest.execute()).thenThrow(new IOException());
        // and
        Response fallbackResponse = mock(Response.class);

        // when
        final Response response = httpCommand()
                .inGroup(TestGroup.TEST)
                .forRequest(mockRequest)
                .withFallback(() -> fallbackResponse)
                .build()
                .execute();
        // then
        assertThat(response, is(fallbackResponse));
    }
}