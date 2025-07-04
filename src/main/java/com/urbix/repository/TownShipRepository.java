package com.urbix.repository;

import com.urbix.entity.Township;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface TownShipRepository extends JpaRepository<Township, Long> {
    List<Township> findByUserId(Long userId);
    List<Township> findByState(String state);
}
