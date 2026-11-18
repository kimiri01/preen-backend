package com.preen.preen_backend.review.spotify;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SpotifyMetadataServiceTests {
    @Test void usesFixedPublicEndpointAndCachesTitleWithoutReturningHtml() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> {assertEquals("open.spotify.com",request.getURI().getHost());assertEquals("/oembed",request.getURI().getPath());})
            .andRespond(withSuccess("{\"title\":\"Blueming\",\"html\":\"untrusted HTML\"}",MediaType.APPLICATION_JSON));
        var service=new SpotifyMetadataService(builder.build(),Clock.systemUTC());
        var result=service.lookup("4Dr2hJ3EnVh2Aaot6fRwDO");
        assertEquals("Blueming",result.title());assertNull(result.artist());
        assertEquals(result,service.lookup("4Dr2hJ3EnVh2Aaot6fRwDO"));server.verify();
    }
    @Test void rejectsUrlsBeforeExternalRequest() {
        var service=new SpotifyMetadataService(RestClient.create(),Clock.systemUTC());
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,()->service.lookup("https://example.com")).getStatusCode());
    }
    @Test void remoteFailureDoesNotExposeResponseBody() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> {}).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        var service=new SpotifyMetadataService(builder.build(),Clock.systemUTC());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ResponseStatusException.class,()->service.lookup("4Dr2hJ3EnVh2Aaot6fRwDO")).getStatusCode());server.verify();
    }
}
