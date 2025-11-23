package ru.mtuci.autonotesbackend.modules.user.impl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthResponseDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.RegistrationRequestDto;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;
import ru.mtuci.autonotesbackend.security.JwtService;
import ru.mtuci.autonotesbackend.security.SecurityUser;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @Transactional
    public AuthResponseDto register(RegistrationRequestDto request) {
        User newUser = userService.createUser(request);

        UserDetails userDetails = new SecurityUser(newUser);
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthResponseDto.builder().token(jwtToken).build();
    }

    public AuthResponseDto login(AuthRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        var user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        UserDetails userDetails = new SecurityUser(user);
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponseDto.builder().token(jwtToken).build();
    }
}
