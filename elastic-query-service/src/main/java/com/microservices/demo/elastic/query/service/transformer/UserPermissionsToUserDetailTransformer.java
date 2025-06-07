package com.microservices.demo.elastic.query.service.transformer;

import com.microservices.demo.elastic.query.service.dataaccess.entity.UserPermission;
import com.microservices.demo.elastic.query.service.security.PermissionType;
import com.microservices.demo.elastic.query.service.security.TwitterQueryUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserPermissionsToUserDetailTransformer {

    public TwitterQueryUser getUserDetails(List<UserPermission> userPermissions) {
        TwitterQueryUser twitterQueryUser = TwitterQueryUser.builder()
                .username(userPermissions.get(0).getUsername())
                .permissions(userPermissions.stream()
                        .collect(Collectors.toMap(
                                UserPermission::getDocumentId,
                                permission -> PermissionType.valueOf(permission.getPermissionType()))))
                .build();

        return twitterQueryUser;           
    }

}
