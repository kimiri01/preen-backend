package com.preen.preen_backend.review.write;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewWriteControllerTests {
    private final ReviewWriteService service=mock(ReviewWriteService.class);
    private final ReviewUserResolver resolver=new ReviewUserResolver(false);
    private final MockMvc mvc=MockMvcBuilders.standaloneSetup(new ReviewWriteController(service,resolver))
        .setControllerAdvice(new ReviewWriteErrorHandler()).build();
    record TestUser(long userId) implements ReviewUserPrincipal { public String getName(){return "test-member";} }
    private final UsernamePasswordAuthenticationToken login=UsernamePasswordAuthenticationToken.authenticated(new TestUser(7),null,List.of());
    @Test void requiresTrustedPrincipalAndDoesNotTrustNumericUsername() throws Exception {
        mvc.perform(get("/api/review-reservations/9")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/review-reservations/9").principal(UsernamePasswordAuthenticationToken.authenticated("7",null,List.of())))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void teamTemporaryPrincipalRequiresExplicitOptInAndUserRole() {
        var temporary = UsernamePasswordAuthenticationToken.authenticated(1L,null,
            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> resolver.requireUserId(temporary));
        var enabled = new ReviewUserResolver(true);
        org.junit.jupiter.api.Assertions.assertEquals(1L,enabled.requireUserId(temporary));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> enabled.requireUserId(UsernamePasswordAuthenticationToken.authenticated("1",null,temporary.getAuthorities())));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> enabled.requireUserId(UsernamePasswordAuthenticationToken.authenticated(1L,null,List.of())));
    }
    @Test void csrfEndpointHandlesTeamModeWithoutToken() throws Exception {
        mvc.perform(get("/api/reviews/csrf").principal(login))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
    @Test void normalModeKeepsCsrfToken() throws Exception {
        var token = new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token");
        mvc.perform(get("/api/reviews/csrf").principal(login)
            .requestAttr(org.springframework.security.web.csrf.CsrfToken.class.getName(),token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.headerName").value("X-CSRF-TOKEN"))
            .andExpect(jsonPath("$.data.token").value("test-token"));
    }
    @Test void createsOnlyUsingServerPrincipalAndDoesNotAcceptOwnerFromBody() throws Exception {
        when(service.create(7,9,5,"4Dr2hJ3EnVh2Aaot6fRwDO")).thenReturn(new ReviewWriteService.Created("10","9","32",5,"4Dr2hJ3EnVh2Aaot6fRwDO"));
        mvc.perform(post("/api/reviews").principal(login).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reservationId\":9,\"rating\":5,\"spotifyTrackId\":\"4Dr2hJ3EnVh2Aaot6fRwDO\",\"userId\":999}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.id").value("10"));
        verify(service).create(7,9,5,"4Dr2hJ3EnVh2Aaot6fRwDO");
    }
    @Test void rejectsInvalidPayloadBeforeSaving() throws Exception {
        for(String body:List.of("{}", "{\"reservationId\":9,\"rating\":4.5,\"spotifyTrackId\":\"4Dr2hJ3EnVh2Aaot6fRwDO\"}","{\"reservationId\":9,\"rating\":0,\"spotifyTrackId\":\"4Dr2hJ3EnVh2Aaot6fRwDO\"}","{\"reservationId\":9,\"rating\":5,\"spotifyTrackId\":\"https://evil.invalid\"}")) {
            mvc.perform(post("/api/reviews").principal(login).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
