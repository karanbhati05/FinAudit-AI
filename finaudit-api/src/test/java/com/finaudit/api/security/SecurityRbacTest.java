package com.finaudit.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finaudit.api.config.SecurityConfig;
import com.finaudit.api.controller.AuthController;
import com.finaudit.api.controller.PolicyAdminController;
import com.finaudit.api.controller.ReportController;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.api.service.AuthService;
import com.finaudit.api.service.PolicyIngestionService;
import com.finaudit.api.service.ReportParsingService;
import com.finaudit.api.service.ReportQueryService;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.AuthResponse;
import com.finaudit.core.model.LoginRequest;
import com.finaudit.core.model.RegisterRequest;
import com.finaudit.core.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PolicyAdminController.class, ReportController.class, AuthController.class})
@Import(SecurityConfig.class)
class SecurityRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PolicyIngestionService policyIngestionService;

    @MockitoBean
    private ReportRepository reportRepository;

    @MockitoBean
    private ReportLineItemRepository lineItemRepository;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private ReportParsingService reportParsingService;

    @MockitoBean
    private AuditRunRepository auditRunRepository;

    @MockitoBean
    private AuditFindingRepository auditFindingRepository;

    @MockitoBean
    private ReportQueryService reportQueryService;

    @MockitoBean
    private com.finaudit.api.service.CostGuardrailService costGuardrailService;

    @Test
    @DisplayName("Unauthenticated request to protected endpoint /api/reports/1 should return 401 Unauthorized")
    void unauthenticatedRequestShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/reports/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Unauthenticated request to admin endpoint /api/admin/policies/reindex should return 401 Unauthorized")
    void unauthenticatedAdminRequestShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/admin/policies/reindex"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Wrong-role request: AUDITOR hitting /api/admin/policies/reindex should return 403 Forbidden")
    @WithMockUser(roles = "AUDITOR")
    void auditorAccessingAdminEndpointShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/admin/policies/reindex"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Wrong-role request: VIEWER hitting /api/reports/upload should return 403 Forbidden")
    @WithMockUser(roles = "VIEWER")
    void viewerUploadingReportShouldReturn403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());

        mockMvc.perform(multipart("/api/reports/upload").file(file))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("ADMIN role should successfully access /api/admin/policies/reindex and return 200 OK")
    @WithMockUser(roles = "ADMIN")
    void adminCanReindexPolicies() throws Exception {
        when(policyIngestionService.ingestSeedPolicies()).thenReturn(10);

        mockMvc.perform(post("/api/admin/policies/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.policyCount").value(10));
    }

    @Test
    @DisplayName("VIEWER role attempting to access another user's report detail should return 403 Forbidden")
    @WithMockUser(username = "viewer@company.com", roles = "VIEWER")
    void viewerCannotSeeAnotherUserReportDetail() throws Exception {
        when(reportQueryService.getReportDetail(99L))
                .thenThrow(new AccessDeniedException("Access denied: Viewers can only view reports where they are the owner or participant."));

        mockMvc.perform(get("/api/reports/99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied: Viewers can only view reports where they are the owner or participant."));
    }

    @Test
    @DisplayName("Public auth endpoints /api/auth/register and /api/auth/login should be accessible without credentials")
    void publicAuthEndpointsShouldBeAccessible() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("new@finaudit.ai", "password123", UserRole.AUDITOR);
        AuthResponse authRes = new AuthResponse("token-123", "refresh-123", 1L, "new@finaudit.ai", UserRole.AUDITOR, 900000L);

        when(authService.register(any())).thenReturn(authRes);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-123"))
                .andExpect(jsonPath("$.email").value("new@finaudit.ai"));

        LoginRequest loginReq = new LoginRequest("new@finaudit.ai", "password123");
        when(authService.login(any())).thenReturn(authRes);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"));
    }
}
