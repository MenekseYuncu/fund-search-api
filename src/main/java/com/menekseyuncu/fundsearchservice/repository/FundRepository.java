package com.menekseyuncu.fundsearchservice.repository;

import com.menekseyuncu.fundsearchservice.model.entity.FundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FundRepository extends JpaRepository<FundEntity, String> {

}
