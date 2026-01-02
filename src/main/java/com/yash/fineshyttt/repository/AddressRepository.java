package com.yash.fineshyttt.repository;

import com.yash.fineshyttt.domain.Address;
import com.yash.fineshyttt.domain.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser_Id(Long userId);

    Optional<Address> findByIdAndUser_Id(Long id, Long userId);

    Optional<Address> findByUser_IdAndIsDefaultTrue(Long userId);

    List<Address> findByUser_IdAndAddressType(Long userId, AddressType addressType);

    long countByUser_Id(Long userId);
}
