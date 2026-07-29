package com.geckofly.messenger.security;

import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login));
        // Анонимизированный аккаунт не пускаем ни по паролю, ни по действующему access-токену.
        if (user.isDeleted()) {
            throw new UsernameNotFoundException("User is deleted: " + login);
        }
        return user;
    }
}
