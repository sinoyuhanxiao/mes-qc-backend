package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.user.TeamForUserTableDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.models.sql.user.Role;
import com.fps.svmes.models.sql.user.TeamUser;
import com.fps.svmes.models.sql.user.User;
import com.fps.svmes.repositories.jpaRepo.user.RoleRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamUserRepository;
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
    private TeamUserRepository teamUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private TeamRepository teamRepository;

    // Get all users
    // TODO: add the teams
    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> {
                    // Map the user entity to UserDTO
                    UserDTO userDTO = modelMapper.map(user, UserDTO.class);

                    // Fetch teams directly using the repository method that returns TeamForUserTableDTO
                    List<TeamForUserTableDTO> teams = teamUserRepository.findTeamsByUserId(user.getId());

                    // Assign the mapped teams to the UserDTO
                    userDTO.setTeams(teams);

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
    // TODO: add the teams
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
            if (userDTO.getRole() != null && userDTO.getRole().getId() != null) {
                // Fetch the Role object from the RoleRepository using the ID
                Optional<Role> roleOptional = roleRepository.findById(userDTO.getRole().getId());

                if (roleOptional.isPresent()) {
                    existingUser.setRole(roleOptional.get());  // Set the fetched Role object
                } else {
                    throw new RuntimeException("Role with ID " + userDTO.getRole().getId() + " not found");
                }
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
