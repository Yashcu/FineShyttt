package com.yash.fineshyttt.mapper;

import com.yash.fineshyttt.domain.User;
import com.yash.fineshyttt.dto.user.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * User Mapper
 *
 * Maps between User entity and UserResponse DTO.
 *
 * MapStruct Configuration:
 * - componentModel = "spring": Generates Spring bean (@Component)
 * - unmappedTargetPolicy = IGNORE: Ignores unmapped fields (lenient)
 *
 * Custom Mappings:
 * - roles: Ignored in toResponse (requires manual mapping)
 * - passwordHash: Never exposed in DTO (security)
 *
 * Future Enhancements:
 * - Add custom role mapping method
 * - Add update methods (merge DTO changes into entity)
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Convert User entity to UserResponse DTO
     *
     * Excludes:
     * - passwordHash (security: never expose password hash)
     * - roles (requires custom mapping)
     *
     * @param user User entity
     * @return UserResponse DTO
     */
    @Mapping(target = "roles", ignore = true)
    UserResponse toResponse(User user);

    // Add more mappings as needed
    // Example: UserProfileResponse toProfileResponse(User user);
}
