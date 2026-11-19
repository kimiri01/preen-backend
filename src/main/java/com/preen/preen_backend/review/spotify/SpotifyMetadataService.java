package com.preen.preen_backend.review.spotify;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Public oEmbed title only. No tokens, downloaded audio, or persistent Spotify metadata. */
@Service
public class SpotifyMetadataService {
    public record Metadata(String title, String artist, String album) {}
    private record Cached(Metadata value,long expiresAt) {}
    private final RestClient client;
    private final Clock clock;
    private final ConcurrentHashMap<String,Cached> cache=new ConcurrentHashMap<>();
    public SpotifyMetadataService() { this(defaultClient(),Clock.systemUTC()); }
    SpotifyMetadataService(RestClient client,Clock clock) {this.client=client;this.clock=clock;}
    private static RestClient defaultClient() {
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(4));
        return RestClient.builder().requestFactory(factory).build();
    }
    public Metadata lookup(String id) {
        if (!id.matches("[a-zA-Z0-9]{22}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var existing=cache.get(id);
        if(existing!=null && existing.expiresAt()>clock.millis())return existing.value();
        try {
            Map<?,?> body=client.get().uri("https://open.spotify.com/oembed?url={url}","https://open.spotify.com/track/"+id).retrieve().body(Map.class);
            if(body==null || !(body.get("title") instanceof String title) || title.isBlank())throw new IllegalStateException();
            var result=new Metadata(title.substring(0,Math.min(title.length(),200)),null,null);
            // Short-lived bounded in-process cache; team policy keeps only track IDs in the DB.
            if(cache.size()>=128)cache.clear();
            cache.put(id,new Cached(result,clock.millis()+Duration.ofMinutes(5).toMillis()));
            return result;
        } catch(Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Spotify 정보를 일시적으로 가져오지 못했습니다.");
        }
    }
}
