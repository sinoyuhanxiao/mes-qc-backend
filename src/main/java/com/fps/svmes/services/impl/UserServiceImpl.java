package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.user.ShiftForUserTableDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.models.sql.user.ShiftUser;
import com.fps.svmes.models.sql.user.User;
import com.fps.svmes.repositories.jpaRepo.user.ShiftRepository;
import com.fps.svmes.repositories.jpaRepo.user.ShiftUserRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftUserRepository shiftUserRepository;

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ShiftRepository shiftRepository;

    // Get all users
    // TODO: add the shifts
    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> {
                    // Map the user entity to UserDTO
                    UserDTO userDTO = modelMapper.map(user, UserDTO.class);

                    // Fetch shifts directly using the repository method that returns ShiftForUserTableDTO
                    List<ShiftForUserTableDTO> shifts = shiftUserRepository.findShiftsByUserId(user.getId());

                    // Assign the mapped shifts to the UserDTO
                    userDTO.setShifts(shifts);

                    return userDTO;
                })
                .collect(Collectors.toList());
    }


    // Create a new user
    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        User user = modelMapper.map(userDTO, User.class);
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDTO.class);
    }

    // Update an existing user
    // TODO: add the shifts
    @Override
    @Transactional
    public UserDTO updateUser(Integer id, UserDTO userDTO) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();

            // Update fields only if they are not null
            if (userDTO.getName() != null) {
                existingUser.setName(userDTO.getName());
            }
            if (userDTO.getRoleId() != null) {
                existingUser.setRoleId(userDTO.getRoleId());
            }
            if (userDTO.getWecomId() != null) {
                existingUser.setWecomId(userDTO.getWecomId());
            }
            if (userDTO.getUsername() != null) {
                existingUser.setUsername(userDTO.getUsername());
            }
            if (userDTO.getPassword() != null) {
                existingUser.setPassword(userDTO.getPassword());
            }
            if (userDTO.getEmail() != null) {
                existingUser.setEmail(userDTO.getEmail());
            }
            if (userDTO.getPhoneNumber() != null) {
                existingUser.setPhoneNumber(userDTO.getPhoneNumber());
            }
            // basic 5
            if (userDTO.getStatus() != null) {
                existingUser.setStatus(userDTO.getStatus());
            }

            if (userDTO.getCreatedBy() != null) {
                existingUser.setCreatedBy(userDTO.getCreatedBy());
            }

            if (userDTO.getUpdatedBy() != null) {
                existingUser.setUpdatedBy(userDTO.getUpdatedBy());
            }

            if (userDTO.getCreatedAt() != null) {
                existingUser.setCreatedAt(userDTO.getCreatedAt());
            }

            if (userDTO.getUpdatedAt() != null) {
                existingUser.setUpdatedAt(userDTO.getUpdatedAt());
            }

            User updatedUser = userRepository.save(existingUser);
            return modelMapper.map(updatedUser, UserDTO.class);
        } else {
            throw new RuntimeException("User with ID " + id + " not found");
        }
    }


    // Delete a user by ID
    @Override
    @Transactional
    public void deleteUser(Integer id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        } else {
            throw new RuntimeException("User with ID " + id + " not found");
        }
    }

    @Override
    @Transactional
    public boolean validateCredentials(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        // Check if the Optional contains a User and compare the passwords
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return password.equals(user.getPassword());
        }

        return false;
    }
    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return modelMapper.map(user, UserDTO.class);
        } else {
            throw new RuntimeException("User with username " + username + " not found");
        }
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Integer id) {
        Optional<User> userOptional = userRepository.findById(id);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return modelMapper.map(user, UserDTO.class);
        } else {
            throw new RuntimeException("User with id " + id  + " not found");
        }
    }

    public List<UserDTO> getUsersByIds(List<Integer> userIds) {
        return getAllUsers().stream()
                .filter(user -> userIds.contains(user.getId()))
                .toList();
    }

}
