package com.chromediopside.controller;

import com.chromediopside.repository.UserRepository;
import com.chromediopside.service.ErrorService;
import com.chromediopside.service.PageService;
import com.chromediopside.service.GiTinderUserService;
import com.chromediopside.service.ProfileService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

  private ProfileService profileService;
  private ErrorService errorService;
  private GiTinderUserService userService;
  private PageService pageService;
  private UserRepository userRepository;

  @Autowired
  public ProfileController(ProfileService profileService,
          ErrorService errorService,
          GiTinderUserService userService,
          PageService pageService, UserRepository userRepository) {
    this.profileService = profileService;
    this.errorService = errorService;
    this.userService = userService;
    this.pageService = pageService;
    this.userRepository = userRepository;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> exception(Exception ex) {
    return new ResponseEntity<>(errorService.unauthorizedRequestError(), HttpStatus.UNAUTHORIZED);
  }

  @CrossOrigin("*")
  @GetMapping("/profile")
  public ResponseEntity<?> getOwnProfile(
          @RequestHeader(name = "X-GiTinder-token") String appToken) {
    if (!userService.validAppToken(appToken)) {
      return new ResponseEntity<>(errorService.unauthorizedRequestError(), HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(profileService.getOwnProfile(appToken), HttpStatus.OK);
  }

  @CrossOrigin("*")
  @RequestMapping("/profiles/{username}")
  public ResponseEntity<?> getOtherProfile(@PathVariable String username,
          @RequestHeader(name = "X-GiTinder-token") String appToken) throws Exception {
    if (!userService.validAppToken(appToken)) {
      return new ResponseEntity<>(errorService.unauthorizedRequestError(), HttpStatus.UNAUTHORIZED);
    }
    if (!userRepository.existsByUserName(username)) {
      return new ResponseEntity<>(errorService.noSuchUserError(), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(profileService.getOtherProfile(appToken, username), HttpStatus.OK);
  }

  @CrossOrigin("*")
  @GetMapping(value = {"/available/{page}", "/available"})
  public ResponseEntity<?> listAvailableProfilesByPage(
          @RequestHeader(name = "X-GiTinder-token") String appToken,
          @PathVariable Optional<Integer> page) throws Exception {
    if (!userService.validAppToken(appToken)) {
      return new ResponseEntity<>(errorService.unauthorizedRequestError(), HttpStatus.UNAUTHORIZED);
    }
    if (page.isPresent()) {
      if (!profileService.enoughProfiles(page.get())) {
        return new ResponseEntity<>(errorService.getNoMoreAvailableProfiles(), HttpStatus.NO_CONTENT);
      }
      return new ResponseEntity<>(profileService.tenProfileByPage(page.get()), HttpStatus.OK) ;
    }
    if (!profileService.enoughProfiles(1)) {
      return new ResponseEntity<>(errorService.getNoMoreAvailableProfiles(), HttpStatus.NO_CONTENT);
    }
    return new ResponseEntity<>(profileService.tenProfileByPage(1), HttpStatus.OK);
  }

  @CrossOrigin("*")
  @PutMapping("/profiles/{username}/{direction}")
  public ResponseEntity<?> swipe(@RequestHeader(name = "X-GiTinder-token") String appToken,
          @PathVariable String username,
          @PathVariable String direction) {
    return new ResponseEntity<>(profileService.handleSwiping(appToken, username, direction), HttpStatus.OK) ;
  }
}
