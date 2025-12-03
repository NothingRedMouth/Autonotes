package ru.mtuci.autonotesbackend.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void doFilter_shouldAddHeaderAndMdc() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        doAnswer(_ -> {
            String requestId = MDC.get("requestId");
            assertNotNull(requestId, "RequestId must be in MDC during execution");
            assertTrue(requestId.matches("^[0-9a-fA-F-]{36}$"), "RequestId must be a UUID");
            return null;
        }).when(chain).doFilter(any(), any());

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        verify(response).setHeader(eq("X-Request-ID"), matches("^[0-9a-fA-F-]{36}$"));
        verify(chain).doFilter(request, response);
        assertNull(MDC.get("requestId"), "MDC must be cleared after execution");
    }
}
