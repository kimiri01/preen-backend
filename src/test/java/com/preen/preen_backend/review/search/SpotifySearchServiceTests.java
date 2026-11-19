package com.preen.preen_backend.review.search;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;

class SpotifySearchServiceTests {
    final Clock clock=Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"),ZoneOffset.UTC);
    final String token="{\"access_token\":\"test-only-token\",\"expires_in\":3600}";
    final String track="{\"id\":\"4Dr2hJ3EnVh2Aaot6fRwDO\",\"name\":\"Song\",\"artists\":[{\"name\":\"Artist\"}],\"album\":{\"name\":\"Album\",\"images\":[{\"url\":\"https://i.scdn.co/image/test\"}]}}";
    String result(String items){return "{\"tracks\":{\"items\":["+items+"]}}";}
    @Test void fixedEndpointsEncodedQuerySanitizedFieldsAndTokenReuse() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(r->{assertEquals("https://accounts.spotify.com/api/token",r.getURI().toString());assertTrue(r.getHeaders().getFirst("Authorization").startsWith("Basic "));}).andRespond(withSuccess(token,MediaType.APPLICATION_JSON));
        for(int i=0;i<2;i++)server.expect(r->{assertEquals("api.spotify.com",r.getURI().getHost());assertTrue(r.getURI().getRawQuery().contains("limit=10"));assertTrue(r.getURI().getRawQuery().contains("%26"));assertEquals("Bearer test-only-token",r.getHeaders().getFirst("Authorization"));}).andRespond(withSuccess(result(track+","+track),MediaType.APPLICATION_JSON));
        var service=new SpotifySearchService(builder.build(),clock,"test-id","test-secret");
        for(int i=0;i<2;i++){var rows=service.search("아이유 & song");assertEquals(1,rows.size());assertEquals("Artist",rows.getFirst().artists().getFirst());assertEquals("https://open.spotify.com/track/4Dr2hJ3EnVh2Aaot6fRwDO",rows.getFirst().spotifyUrl());}
        server.verify();
    }
    @Test void missingConfigAndInvalidQueriesNeverCallExternalService() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();var service=new SpotifySearchService(builder.build(),clock,"","");
        assertEquals("SPOTIFY_NOT_CONFIGURED",assertThrows(SpotifySearchService.Failure.class,()->service.search("song")).code);
        for(String q:new String[]{" ","a".repeat(101),"bad\nquery"})assertEquals(400,assertThrows(SpotifySearchService.Failure.class,()->service.search(q)).status);
        server.verify();
    }
    @Test void expiredSpotifyTokenRetriesOnlyOnce() {
        var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();
        server.expect(r->{}).andRespond(withSuccess(token,MediaType.APPLICATION_JSON));
        server.expect(r->{}).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(r->{}).andRespond(withSuccess(token,MediaType.APPLICATION_JSON));
        server.expect(r->{}).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("provider-private-body"));
        var service=new SpotifySearchService(b.build(),clock,"test-id","test-secret");
        var failure=assertThrows(SpotifySearchService.Failure.class,()->service.search("song"));assertEquals(503,failure.status);assertFalse(failure.toString().contains("provider-private-body"));server.verify();
    }
    @Test void providerLimitFailureAndEmptyResultsAreDifferent() {
        for(int status:new int[]{200,429,500}) {
            var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();
            server.expect(r->{}).andRespond(withSuccess(token,MediaType.APPLICATION_JSON));
            server.expect(r->{}).andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON).body(status==200?result(""):"sensitive-provider-body"));
            var service=new SpotifySearchService(b.build(),clock,"test-id","test-secret");
            if(status==200)assertTrue(service.search("song").isEmpty());else assertEquals(status==429?429:503,assertThrows(SpotifySearchService.Failure.class,()->service.search("song")).status);
            server.verify();
        }
    }
    @Test void invalidTracksAndUnsafeImagesAreNotReturnedAsLinks() {
        var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();server.expect(r->{}).andRespond(withSuccess(token,MediaType.APPLICATION_JSON));
        server.expect(r->{}).andRespond(withSuccess(result(track.replace("https://i.scdn.co/image/test","http://private.example/image")+",{\"id\":\"bad\",\"name\":\"bad\"}"),MediaType.APPLICATION_JSON));
        var rows=new SpotifySearchService(b.build(),clock,"test-id","test-secret").search("song");assertEquals(1,rows.size());assertNull(rows.getFirst().imageUrl());server.verify();
    }
    @Test void capsResultsAtTenAndDeduplicatesIds() {
        var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();
        server.expect(r->{}).andRespond(withSuccess(token,MediaType.APPLICATION_JSON));
        var items=new java.util.ArrayList<String>();
        for(int i=1;i<=12;i++)items.add(track.replace("4Dr2hJ3EnVh2Aaot6fRwDO",String.format("%022d",i)));
        server.expect(r->{}).andRespond(withSuccess(result(String.join(",",items)),MediaType.APPLICATION_JSON));
        assertEquals(10,new SpotifySearchService(b.build(),Clock.systemUTC(),"test-id","test-secret").search("song").size());server.verify();
    }

}
