package ru.mtuci.autonotesbackend.app.web.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.autonotesbackend.modules.user.api.UserFacade;
import ru.mtuci.autonotesbackend.modules.user.api.dto.UserProfileDto;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserResource {

    private final UserFacade userApi;

    @Override
    @GetMapping("/{username}")
    @PreAuthorize("authentication.name == #username")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(userApi.getProfile(username));
    }
}
