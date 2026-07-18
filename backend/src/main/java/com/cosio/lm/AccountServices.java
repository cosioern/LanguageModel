package com.cosio.lm;

import org.springframework.stereotype.Service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class AccountServices {
    
    private final GuestRepository guestRepo;

    private final UserRepository userRepo;

    public AccountServices(GuestRepository guestRepo, UserRepository userRepo) {
        this.guestRepo = guestRepo;
        this.userRepo = userRepo;
    }

    // user registration
    public void createUser(String username, String email, String password) {
        String hash = new BCryptPasswordEncoder().encode(password);
        User user = new User(username, email, hash);
        userRepo.save(user);
    }


    // authentication
    private boolean authenticate(String username, String passowrd) {
        Optional<User> user = userRepo.findByUsername(username);
        if (user.isEmpty())
            return false;

        String hash = new BCryptPasswordEncoder().encode(passowrd);
        if (user.get().getPassword() == hash)
            return false;

        return true;
    }

    // guest -> user migration?

    // profile updates

}
