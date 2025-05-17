package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.models.sql.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u.name FROM User u WHERE u.id = :id")
    String findNameById(@Param("id") Integer id);
}
