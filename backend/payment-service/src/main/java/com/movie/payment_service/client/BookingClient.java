package com.movie.payment_service.client;

import java.util.UUID;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.movie.payment_service.error.PaymentConflictException;
import com.movie.payment_service.error.PaymentDependencyException;

@Component
public class BookingClient {
    private final RestClient restClient;

    public BookingClient(RestClient bookingRestClient) {
        this.restClient = bookingRestClient;
    }

    public BookingPaymentQuote startPayment(UUID bookingId, String ownerSubject) {
        try {
            BookingPaymentQuote quote = restClient.post()
                    .uri("/internal/v1/bookings/{bookingId}/payment-intent", bookingId)
                    .header("X-ShowHub-Owner", ownerSubject)
                    .retrieve()
                    .onStatus(status -> status.value() == 404 || status.value() == 409,
                            (_request, _response) -> {
                                throw new PaymentConflictException("Booking is unavailable or its seat hold expired.");
                            })
                    .onStatus(HttpStatusCode::isError, (_request, response) -> {
                        throw new PaymentDependencyException(
                                "Booking Service rejected payment validation with status "
                                        + response.getStatusCode().value() + ".");
                    })
                    .body(BookingPaymentQuote.class);
            if (quote == null) throw new PaymentDependencyException("Booking Service returned an empty quote.");
            return quote;
        } catch (PaymentConflictException | PaymentDependencyException error) {
            throw error;
        } catch (RestClientException error) {
            throw new PaymentDependencyException("Booking Service is temporarily unavailable.", error);
        }
    }
}
