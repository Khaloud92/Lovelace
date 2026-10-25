package com.sep.sep_backend.FriendshipTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.sep_backend.auth.JwtService;
import com.sep.sep_backend.config.SecurityConfig;
import com.sep.sep_backend.friendship.Friendship;
import com.sep.sep_backend.friendship.FriendshipController;
import com.sep.sep_backend.friendship.FriendshipService;
import com.sep.sep_backend.friendship.dto.RespondRequestDto;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = FriendshipController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))

class FriendshipControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private FriendshipService friendshipService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUpSecurityContext() {
        // ---- ARRANGE (Global) ----
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_EMAIL, null, List.of()));
        SecurityContextHolder.setContext(context);

        AppUser testUser = new AppUser();
        testUser.setId(UUID.fromString(TEST_USER_ID));
        testUser.setEmail(TEST_EMAIL);
        lenient().when(appUserRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void respond_shouldReturnBadRequest_whenServiceThrowsException() throws Exception {
        // ---- ARRANGE ----
        UUID friendshipId = UUID.randomUUID();
        RespondRequestDto dto = new RespondRequestDto(friendshipId, Friendship.Status.ACCEPTED);

        when(friendshipService.respondToRequest(any(AppUser.class), eq(dto)))
                .thenThrow(new IllegalArgumentException("Friend request not found"));

        // ---- ACT & ASSERT ----
        mockMvc.perform(post("/api/friendships/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Friend request not found"));
    }
}