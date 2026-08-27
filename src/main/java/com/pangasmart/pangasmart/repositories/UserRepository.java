package com.pangasmart.pangasmart.repositories;

import com.pangasmart.pangasmart.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Inatafuta mtumiaji mmoja kwa email (Inatumika kwenye Login na usajili)
    Optional<User> findByEmail(String email);

    // Inatafuta orodha ya watumiaji wote wenye email inayofanana (Inazuia Error 500 kwenye AdminController)
    List<User> findAllByEmail(String email);

    // Njia ya kuchuja watumiaji kwa Role (TENANT / LANDLORD / ADMIN)
    List<User> findByRole(String role);
}