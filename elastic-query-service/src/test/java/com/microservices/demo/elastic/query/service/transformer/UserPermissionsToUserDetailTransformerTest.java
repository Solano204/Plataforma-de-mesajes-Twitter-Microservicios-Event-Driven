package com.microservices.demo.elastic.query.service.transformer;

import com.microservices.demo.elastic.query.service.dataaccess.entity.UserPermission;
import com.microservices.demo.elastic.query.service.security.PermissionType;
import com.microservices.demo.elastic.query.service.security.TwitterQueryUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPermissionsToUserDetailTransformerTest {

    private final UserPermissionsToUserDetailTransformer transformer = new UserPermissionsToUserDetailTransformer();

    private UserPermission permission(String username, String documentId, String type) {
        UserPermission permission = new UserPermission();
        permission.setId(UUID.randomUUID());
        permission.setUsername(username);
        permission.setDocumentId(documentId);
        permission.setPermissionType(type);
        return permission;
    }

    @Test
    void getUserDetails_singlePermission_buildsUserWithThatUsernameAndOneEntryMap() {
        TwitterQueryUser user = transformer.getUserDetails(List.of(permission("alice", "doc-1", "READ")));

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPermissions()).containsExactly(java.util.Map.entry("doc-1", PermissionType.READ));
    }

    @Test
    void getUserDetails_multiplePermissions_mapsEachDocumentIdToItsPermissionType() {
        TwitterQueryUser user = transformer.getUserDetails(List.of(
                permission("alice", "doc-1", "READ"),
                permission("alice", "doc-2", "WRITE"),
                permission("alice", "doc-3", "ADMIN")));

        assertThat(user.getPermissions()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                "doc-1", PermissionType.READ,
                "doc-2", PermissionType.WRITE,
                "doc-3", PermissionType.ADMIN));
    }

    @Test
    void getUserDetails_takesUsernameFromTheFirstEntryOnly() {
        // real rows are always for one username per findAllPermissionsByUsername() call,
        // but the transformer itself only ever reads userPermissions.get(0)
        TwitterQueryUser user = transformer.getUserDetails(List.of(
                permission("alice", "doc-1", "READ"),
                permission("someone-else", "doc-2", "READ")));

        assertThat(user.getUsername()).isEqualTo("alice");
    }

    @Test
    void getUserDetails_unknownPermissionTypeString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> transformer.getUserDetails(List.of(permission("alice", "doc-1", "NOT_A_REAL_TYPE"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getUserDetails_emptyList_throwsIndexOutOfBounds() {
        assertThatThrownBy(() -> transformer.getUserDetails(List.of()))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
