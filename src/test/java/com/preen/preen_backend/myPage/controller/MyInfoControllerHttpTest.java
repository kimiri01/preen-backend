package com.preen.preen_backend.myPage.controller;

import com.preen.preen_backend.myPage.service.MyInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MyInfoControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 로그인하지_않으면_내_정보를_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/my/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그인하지_않으면_내_정보를_수정할_수_없다() throws Exception {
        mockMvc.perform(put("/api/my/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "홍길동",
                                    "birthDate": "2000-01-01"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}