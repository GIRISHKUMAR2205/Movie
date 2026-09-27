package com.movie.user_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.movie.user_service.entity.User;



public interface UserRepository extends JpaRepository<User,Long>{

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrUserName(String email, String userName);

    boolean existsByEmailAndIdNot(String email, Long id);

        @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.privileges
        WHERE u.id = :id
    """)
    Optional<User> findByIdWithRoleAndPrivilege(@Param("id") Long id);

     @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.oauthAccounts oa
        WHERE u.email = :email OR(
        oa.providerSubject = :email)
    """)
    Optional<User> findByEmailOrProviderSubject(@Param("email") String email);
}
