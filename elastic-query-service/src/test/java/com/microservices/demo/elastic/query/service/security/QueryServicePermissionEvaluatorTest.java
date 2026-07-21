package com.microservices.demo.elastic.query.service.security;

import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceRequestModel;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceResponseModel;
import com.microservices.demo.elastic.query.service.model.ElasticQueryServiceAnalyticsResponseModel;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServicePermissionEvaluatorTest {

    private static final String SUPER_USER_ROLE = "APP_SUPER_USER_ROLE";

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private Authentication authentication;

    private QueryServicePermissionEvaluator evaluator() {
        return new QueryServicePermissionEvaluator(httpServletRequest);
    }

    private TwitterQueryUser userWithPermissions(Map<String, PermissionType> permissions) {
        return TwitterQueryUser.builder().username("user-1").permissions(permissions).build();
    }

    private void asNonSuperUser() {
        when(httpServletRequest.isUserInRole(SUPER_USER_ROLE)).thenReturn(false);
    }

    // --- hasPermission(Authentication, Object targetDomain, Object permission) ---

    @Test
    void hasPermission_superUser_returnsTrueRegardlessOfTargetDomain() {
        when(httpServletRequest.isUserInRole(SUPER_USER_ROLE)).thenReturn(true);

        boolean result = evaluator().hasPermission(authentication, "anything", "READ");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_requestModel_userHasMatchingPermission_returnsTrue() {
        asNonSuperUser();
        when(authentication.getPrincipal()).thenReturn(userWithPermissions(Map.of("doc-1", PermissionType.READ)));
        ElasticQueryServiceRequestModel target = ElasticQueryServiceRequestModel.builder().id("doc-1").text("hi").build();

        boolean result = evaluator().hasPermission(authentication, target, "READ");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_requestModel_userHasDifferentPermissionType_returnsFalse() {
        asNonSuperUser();
        when(authentication.getPrincipal()).thenReturn(userWithPermissions(Map.of("doc-1", PermissionType.READ)));
        ElasticQueryServiceRequestModel target = ElasticQueryServiceRequestModel.builder().id("doc-1").text("hi").build();

        boolean result = evaluator().hasPermission(authentication, target, "ADMIN");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_requestModel_userHasNoPermissionEntryForThatId_returnsFalse() {
        asNonSuperUser();
        when(authentication.getPrincipal()).thenReturn(userWithPermissions(Map.of("doc-1", PermissionType.READ)));
        ElasticQueryServiceRequestModel target = ElasticQueryServiceRequestModel.builder().id("doc-99").text("hi").build();

        boolean result = evaluator().hasPermission(authentication, target, "READ");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_nullTargetDomain_returnsTrueWithoutCheckingAnyPermission() {
        asNonSuperUser();

        boolean result = evaluator().hasPermission(authentication, null, "READ");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_responseEntityWrappingAnalyticsModel_allEmbeddedIdsAuthorized_returnsTrue() {
        asNonSuperUser();
        when(authentication.getPrincipal()).thenReturn(userWithPermissions(Map.of(
                "doc-1", PermissionType.READ,
                "doc-2", PermissionType.READ
        )));
        ElasticQueryServiceAnalyticsResponseModel body = ElasticQueryServiceAnalyticsResponseModel.builder()
                .wordCount(2L)
                .queryResponseModels(List.of(
                        ElasticQueryServiceResponseModel.builder().id("doc-1").build(),
                        ElasticQueryServiceResponseModel.builder().id("doc-2").build()))
                .build();

        boolean result = evaluator().hasPermission(authentication, ResponseEntity.ok(body), "READ");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_responseEntityWrappingAnalyticsModel_oneEmbeddedIdUnauthorized_returnsFalse() {
        asNonSuperUser();
        when(authentication.getPrincipal()).thenReturn(userWithPermissions(Map.of("doc-1", PermissionType.READ)));
        ElasticQueryServiceAnalyticsResponseModel body = ElasticQueryServiceAnalyticsResponseModel.builder()
                .wordCount(2L)
                .queryResponseModels(List.of(
                        ElasticQueryServiceResponseModel.builder().id("doc-1").build(),
                        ElasticQueryServiceResponseModel.builder().id("doc-not-owned").build()))
                .build();

        boolean result = evaluator().hasPermission(authentication, ResponseEntity.ok(body), "READ");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_responseEntityWrappingAListDirectly_checksEveryElement() {
        asNonSuperUser();
        when(authentication.getPrincipal()).thenReturn(userWithPermissions(Map.of("doc-1", PermissionType.READ)));
        List<ElasticQueryServiceResponseModel> body = List.of(
                ElasticQueryServiceResponseModel.builder().id("doc-1").build());

        boolean result = evaluator().hasPermission(authentication, ResponseEntity.ok(body), "READ");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_responseEntityWithUnrecognizedBodyType_returnsFalse() {
        asNonSuperUser();

        boolean result = evaluator().hasPermission(authentication, ResponseEntity.ok("just a string"), "READ");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_responseEntityWithNullBody_throwsNullPointerException() {
        asNonSuperUser();
        ResponseEntity<Object> emptyBody = ResponseEntity.ok().build();

        assertThatThrownBy(() -> evaluator().hasPermission(authentication, emptyBody, "READ"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void hasPermission_unrecognizedTargetDomainType_returnsFalse() {
        asNonSuperUser();

        boolean result = evaluator().hasPermission(authentication, 12345, "READ");

        assertThat(result).isFalse();
    }

    // --- hasPermission(Authentication, Serializable targetId, String targetType, Object permission) ---

    @Test
    void hasPermissionById_superUser_returnsTrue() {
        when(httpServletRequest.isUserInRole(SUPER_USER_ROLE)).thenReturn(true);

        boolean result = evaluator().hasPermission(authentication, "doc-1", "document", "READ");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermissionById_nullTargetId_returnsFalse() {
        asNonSuperUser();

        boolean result = evaluator().hasPermission(authentication, null, "document", "READ");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermissionById_matchingPermission_returnsTrue() {
        asNonSuperUser();
        when(authentication.getPrincipal()).thenReturn(userWithPermissions(Map.of("doc-1", PermissionType.WRITE)));

        boolean result = evaluator().hasPermission(authentication, "doc-1", "document", "WRITE");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermissionById_wrongPermissionType_returnsFalse() {
        asNonSuperUser();
        when(authentication.getPrincipal()).thenReturn(userWithPermissions(Map.of("doc-1", PermissionType.WRITE)));

        boolean result = evaluator().hasPermission(authentication, "doc-1", "document", "READ");

        assertThat(result).isFalse();
    }
}
