package com.preen.preen_backend.review.search;

import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import tools.jackson.databind.JsonNode;

@Service
public class SpotifySearchService {
    public record Track(String id,String name,List<String> artists,String albumName,String imageUrl,String spotifyUrl) {}
    public static class Failure extends RuntimeException {
        public final int status; public final String code;
        Failure(int status,String code,String message) {super(message);this.status=status;this.code=code;}
    }
    private final RestClient client; private final Clock clock;
    private final String clientId,clientSecret;
    private String token; private Instant expires=Instant.EPOCH;

    @Autowired public SpotifySearchService(Clock clock,
        @Value("${app.spotify.client-id:${SPOTIFY_CLIENT_ID:}}") String id,
        @Value("${app.spotify.client-secret:${SPOTIFY_CLIENT_SECRET:}}") String secret) {
        this(defaultClient(),clock,id,secret);
    }
    SpotifySearchService(RestClient client,Clock clock,String id,String secret) {
        this.client=client;this.clock=clock;this.clientId=id;this.clientSecret=secret;
    }
    private static RestClient defaultClient() {
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(6));
        return RestClient.builder().requestFactory(factory).build();
    }
    private synchronized String appToken() {
        if(token!=null && clock.instant().isBefore(expires))return token;
        try {
            JsonNode body=client.post().uri("https://accounts.spotify.com/api/token")
                .headers(h->h.setBasicAuth(clientId,clientSecret,StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials").retrieve().body(JsonNode.class);
            if(body==null || body.path("access_token").asText().isBlank() || body.path("expires_in").asLong()<=0)throw new IllegalArgumentException();
            token=body.path("access_token").asText();
            long lifetime=Math.min(body.path("expires_in").asLong(),86400);
            expires=clock.instant().plusSeconds(Math.max(1,lifetime-30));
            return token;
        } catch (RestClientResponseException e) {throw remoteFailure(e.getStatusCode().value());}
          catch (RuntimeException e) {throw new Failure(503,"SPOTIFY_UNAVAILABLE","음악 검색 서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.");}
    }
    private synchronized void invalidate(String rejected) { if(Objects.equals(token,rejected)){token=null;expires=Instant.EPOCH;} }
    public List<Track> search(String input) {
        String query=input==null?"":input.strip();
        if(query.isBlank() || query.codePointCount(0,query.length())>100 || query.chars().anyMatch(Character::isISOControl))
            throw new Failure(400,"SPOTIFY_QUERY_INVALID","검색어를 1~100자로 입력해주세요.");
        if(clientId.isBlank() || clientSecret.isBlank())throw new Failure(503,"SPOTIFY_NOT_CONFIGURED","음악 검색이 설정되지 않았습니다.");
        for(int attempt=0;attempt<2;attempt++) {
            String value=appToken();
            try {
                JsonNode body=client.get().uri(URI.create("https://api.spotify.com/v1/search?type=track&limit=10&market=KR&q="+URLEncoder.encode(query,StandardCharsets.UTF_8)))
                    .headers(h->h.setBearerAuth(value)).retrieve().body(JsonNode.class);
                if(body==null || !body.path("tracks").path("items").isArray())throw new IllegalArgumentException();
                Map<String,Track> result=new LinkedHashMap<>();
                for(JsonNode item:body.path("tracks").path("items")) {
                    String id=item.path("id").asText(),name=item.path("name").asText();
                    if(!id.matches("[A-Za-z0-9]{22}") || name.isBlank() || item.path("is_local").asBoolean())continue;
                    List<String> artists=new ArrayList<>();
                    for(JsonNode artist:item.path("artists")) if(!artist.path("name").asText().isBlank())artists.add(artist.path("name").asText());
                    String image=null;
                    for(JsonNode img:item.path("album").path("images")) {
                        String candidate=img.path("url").asText();
                        if(candidate.startsWith("https://i.scdn.co/image/")){image=candidate;break;}
                    }
                    result.putIfAbsent(id,new Track(id,name,List.copyOf(artists),item.path("album").path("name").asText(),image,"https://open.spotify.com/track/"+id));
                    if(result.size()==10)break;
                }
                return List.copyOf(result.values());
            } catch(RestClientResponseException e) {
                if(e.getStatusCode().value()==401 && attempt==0){invalidate(value);continue;}
                throw remoteFailure(e.getStatusCode().value());
            } catch(RuntimeException e) {throw new Failure(503,"SPOTIFY_UNAVAILABLE","음악 검색 서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.");}
        }
        throw new Failure(503,"SPOTIFY_UNAVAILABLE","음악 검색을 완료하지 못했습니다.");
    }
    private static Failure remoteFailure(int status) {
        if(status==429)return new Failure(429,"SPOTIFY_RATE_LIMITED","음악 검색 요청이 많습니다. 잠시 후 다시 시도해주세요.");
        return new Failure(503,"SPOTIFY_UNAVAILABLE","음악 검색을 이용할 수 없습니다. 설정과 서비스 상태를 확인해주세요.");
    }
}
