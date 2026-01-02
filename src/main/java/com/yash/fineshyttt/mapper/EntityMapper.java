package com.yash.fineshyttt.mapper;

import java.util.List;

/**
 * Base Mapper Interface
 *
 * Generic mapper interface for common mapping operations.
 * Reduces boilerplate in specific mapper interfaces.
 *
 * Type Parameters:
 * - E: Entity type (domain model)
 * - D: DTO type (response/request model)
 *
 * Usage:
 * public interface UserMapper extends EntityMapper<User, UserResponse> {
 *     // Additional mappings...
 * }
 */
public interface EntityMapper<E, D> {

    /**
     * Convert entity to DTO
     *
     * @param entity Entity to convert
     * @return DTO representation
     */
    D toDto(E entity);

    /**
     * Convert DTO to entity
     *
     * @param dto DTO to convert
     * @return Entity representation
     */
    E toEntity(D dto);

    /**
     * Convert list of entities to list of DTOs
     *
     * @param entities Entities to convert
     * @return List of DTOs
     */
    List<D> toDtoList(List<E> entities);

    /**
     * Convert list of DTOs to list of entities
     *
     * @param dtos DTOs to convert
     * @return List of entities
     */
    List<E> toEntityList(List<D> dtos);
}
