package com.preen.preen_backend.review.spotify;

import com.preen.preen_backend.common.security.SecurityRuleContributor;
import org.springframework.stereotype.Component;

/** Public review titles use the same anonymous access as the public review page. */
@Component
public class SpotifySecurityRules implements SecurityRuleContributor {
    @Override
    public String[] publicGetPaths() {
        return new String[] {"/api/spotify/tracks/*/metadata"};
    }
}
