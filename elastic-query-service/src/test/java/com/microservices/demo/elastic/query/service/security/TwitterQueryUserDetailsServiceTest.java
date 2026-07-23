package com.microservices.demo.elastic.query.service.security;

import com.microservices.demo.elastic.query.service.business.QueryUserService;
import com.microservices.demo.elastic.query.service.dataaccess.entity.UserPermission;
import com.microservices.demo.elastic.query.service.transformer.UserPermissionsToUserDetailTransformer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterQueryUserDetailsServiceTest {

    @Mock
    private QueryUserService queryUserService;

    private final UserPermissionsToUserDetailTransformer transformer = new UserPermissionsToUserDetailTransformer();

    private TwitterQueryUserDetailsService service() {
        return new TwitterQueryUserDetailsService(queryUserService, transformer);
    }

    @Test
    void loadUserByUsername_userExists_returnsTransformedUserDetails() {
        UserPermission permission = new UserPermission();
        permission.setUsername("alice");
        permission.setDocumentId("doc-1");
        permission.setPermissionType("READ");
        when(queryUserService.findAllPermissionsByUsername("alice")).thenReturn(Optional.of(List.of(permission)));

        UserDetails result = service().loadUserByUsername("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        when(queryUserService.findAllPermissionsByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
