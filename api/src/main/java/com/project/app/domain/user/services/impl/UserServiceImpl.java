package com.project.app.domain.user.services.impl;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.project.app.domain.user.db.UserRepository;
import com.project.app.domain.user.exception.UserException;
import com.project.app.domain.user.model.User;
import com.project.app.domain.user.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = repository.findByEmail(email);
        if (user.isPresent()) {
            User thisUser = user.get();
            ArrayList<GrantedAuthority> authorities = new ArrayList<>();
            GrantedAuthority thisRole = new SimpleGrantedAuthority(thisUser.getRole().toString());
            authorities.add(thisRole);
            if (!thisUser.getModuleAccess().isEmpty()) {
                thisUser.getModuleAccess().forEach(ma -> {
                    authorities.add(new SimpleGrantedAuthority(ma.name()));
                });
            }
            return new org.springframework.security.core.userdetails.User(email, thisUser.getEncodedPassword(),
                    authorities);
        } else {
            throw new UsernameNotFoundException(String.format("User [%s] not found", email));
        }
    }

    @Override
    public User createUser(User user) {
        return repository.save(user);
    }

    @Override
    public User getById(String id) throws UserException {
        return repository.findById(id).orElseThrow(() -> new UserException("user.error.notfound"));
    }

    @Override
    public User getByUsername(String username) throws UserException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getByUsername'");
    }

    @Override
    public boolean emailExists(String email) {
        Optional<User> userOptional = repository.findByEmail(email);
        return userOptional.isPresent();
    }

    @Override
    public User getCurrentUser(String email) {
        return repository.findByEmail(email).orElse(null);
    }

}
