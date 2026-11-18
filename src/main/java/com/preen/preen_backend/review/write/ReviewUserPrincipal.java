package com.preen.preen_backend.review.write;

import java.security.Principal;

/** Implement on the trusted principal installed by the team's login filter. Never bind from a request. */
public interface ReviewUserPrincipal extends Principal {
    long userId();
}
