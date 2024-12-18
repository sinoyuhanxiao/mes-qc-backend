package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.user.UserDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService {
    List<UserDTO> getAllUsers();

    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(Integer id, UserDTO userDTO);

    void deleteUser(Integer id);

    boolean validateCredentials(String username, String password);

    UserDTO getUserByUsername(String username);
}
