package io.spring.api;

import io.spring.api.exception.InvalidRequestException;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.api.response.ResponseWrappers;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "profiles/{username}")
@AllArgsConstructor
public class ProfileApi {
  private ProfileQueryService profileQueryService;
  private UserRepository userRepository;

  @GetMapping
  public ResponseEntity getProfile(
      @PathVariable("username") String username, @AuthenticationPrincipal User user) {
    return profileQueryService
        .findByUsername(username, user)
        .map(this::profileResponse)
        .orElseThrow(ResourceNotFoundException::new);
  }

  @PostMapping(path = "follow")
  public ResponseEntity follow(
      @PathVariable("username") String username, @AuthenticationPrincipal User user) {
    
    if (user.getUsername().equals(username)) {
      throw new InvalidRequestException("Cannot follow yourself");
    }
    
    return userRepository
        .findByUsername(username)
        .map(target -> {
          if (userRepository.findRelation(user.getId(), target.getId()).isPresent()) {
            throw new InvalidRequestException("Already following this user");
          }
          
          FollowRelation followRelation = new FollowRelation(user.getId(), target.getId());
          userRepository.saveRelation(followRelation);
          return profileResponse(profileQueryService.findByUsername(username, user).get());
        })
        .orElseThrow(ResourceNotFoundException::new);
  }

  @DeleteMapping(path = "follow")
  public ResponseEntity unfollow(
      @PathVariable("username") String username, @AuthenticationPrincipal User user) {
    
    Optional<User> userOptional = userRepository.findByUsername(username);
    if (userOptional.isPresent()) {
      User target = userOptional.get();
      return userRepository
          .findRelation(user.getId(), target.getId())
          .map(relation -> {
            if (!relation.getUserId().equals(user.getId())) {
              throw new NoAuthorizationException();
            }
            
            userRepository.removeRelation(relation);
            return profileResponse(profileQueryService.findByUsername(username, user).get());
          })
          .orElseThrow(ResourceNotFoundException::new);
    } else {
      throw new ResourceNotFoundException();
    }
  }

  private ResponseEntity profileResponse(ProfileData profile) {
    return ResponseEntity.ok(new ResponseWrappers.ProfileResponse(profile));
  }
}
