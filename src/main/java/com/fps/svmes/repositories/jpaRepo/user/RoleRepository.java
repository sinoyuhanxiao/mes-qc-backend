package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.models.sql.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {

    // Example of a custom query method (if needed)
    Role findByName(String name);
}