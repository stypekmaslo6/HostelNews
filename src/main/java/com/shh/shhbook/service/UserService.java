package com.shh.shhbook.service;

import com.shh.shhbook.model.Users;
import com.shh.shhbook.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UsersRepository userRepository;
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username) != null;
    }
    public void addUser(String username) {
        Users user = new Users();
        user.setUsername(username);
        user.setPassword(username);
        userRepository.save(user);
    }

    public void deleteUser(String username) {
        Users user = userRepository.findByUsername(username);
        if (user != null) {
            userRepository.delete(user);
        }
    }
}
