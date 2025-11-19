package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Societe;

import java.util.Optional;

public interface SocieteRepository extends JpaRepository<Societe, Long> {

    Optional<Societe> findByIce(String ice);

}
