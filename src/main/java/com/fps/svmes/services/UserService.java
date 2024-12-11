package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.user.UserDTO;

import java.util.List;

public interface UserService {
    List<UserDTO> getAllUsers();
}
