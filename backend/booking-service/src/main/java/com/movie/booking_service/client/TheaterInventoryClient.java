package com.movie.booking_service.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.movie.booking_service.error.BookingConflictException;
import com.movie.booking_service.error.BookingNotFoundException;
import com.movie.booking_service.error.DownstreamServiceException;

@Component
public class TheaterInventoryClient {
    private final RestClient theaterRestClient;

    public TheaterInventoryClient(RestClient theaterRestClient) {
        this.theaterRestClient = theaterRestClient;
    }

    public TheaterInventoryResponse inventory(Long showId) {
        try {
            TheaterInventoryResponse response = theaterRestClient.get()
                    .uri("/internal/v1/shows/{showId}", showId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (_request, _response) -> {
                        throw new BookingNotFoundException("Show not found.");
                    })
                    .onStatus(status -> status.value() == 409, (_request, _response) -> {
                        throw new BookingConflictException("This show can no longer be booked.");
                    })
                    .onStatus(HttpStatusCode::isError, (_request, responseError) -> {
                        throw new DownstreamServiceException(
                                "Theater inventory rejected the request with status " + responseError.getStatusCode().value() + ".");
                    })
                    .body(TheaterInventoryResponse.class);
            if (response == null) throw new DownstreamServiceException("Theater inventory returned an empty response.");
            return response;
        } catch (BookingNotFoundException | BookingConflictException | DownstreamServiceException error) {
            throw error;
        } catch (RestClientException error) {
            throw new DownstreamServiceException("Theater inventory is temporarily unavailable.", error);
        }
    }
}
