package com.chromediopside.service;

import com.chromediopside.mockbuilder.MockProfileBuilder;
import com.chromediopside.model.GiTinderProfile;
import com.chromediopside.model.GiTinderUser;
import com.chromediopside.model.Language;
import com.chromediopside.repository.ProfileRepository;
import com.chromediopside.repository.UserRepository;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private static final String GET_REQUEST_IOERROR
      = "Some GitHub data of this user is not available for this token!";

  private UserRepository userRepository;
  private ProfileRepository profileRepository;
  private ErrorService errorService;
  private MockProfileBuilder mockProfileBuilder;
  private GiTinderProfile giTinderProfile;

  @Autowired
  public ProfileService(
      UserRepository userRepository,
      ProfileRepository profileRepository,
      ErrorService errorService,
      MockProfileBuilder mockProfileBuilder,
      GiTinderProfile giTinderProfile) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
    this.errorService = errorService;
    this.mockProfileBuilder = mockProfileBuilder;
    this.giTinderProfile = giTinderProfile;
  }

  public ProfileService() {
  }

  public List<GiTinderProfile> randomTenProfileByLanguage(String languageName) {
    return profileRepository.selectTenRandomLanguageName(languageName);
  }

  private GitHubClient setUpGitHubClient(String accessToken) {
    GitHubClient gitHubClient = new GitHubClient();
    gitHubClient.setOAuth2Token(accessToken);
    return gitHubClient;
  }

  private void setLoginAndAvatar(GitHubClient gitHubClient, String username) {
    UserService userService = new UserService(gitHubClient);
    try {
      User user = userService.getUser(username);
      giTinderProfile.setLogin(user.getLogin());
      giTinderProfile.setAvatarUrl(user.getAvatarUrl());
    } catch (IOException e) {
      System.out.println(GET_REQUEST_IOERROR);
    }
  }

  private void setEverythingElse(GitHubClient gitHubClient, String username) {
    RepositoryService repositoryService = new RepositoryService(gitHubClient);
    try {
      List<Repository> repositoryList = repositoryService.getRepositories(username);
      List<String> repos = new ArrayList<>();
      List<String> languages = new ArrayList<>();
      for (Repository currentRepo : repositoryList) {
        repos.add(currentRepo.getName());
        String repoLanguage = currentRepo.getLanguage();
        if (!languages.contains(repoLanguage)) {
          languages.add(repoLanguage);
        }
      }
      giTinderProfile.setRepos(String.join(";", repos));
      Set<Language> languageObjects = new HashSet<>();
      for (String currentLanguage : languages) {
        languageObjects.add(new Language(currentLanguage));
      }
      giTinderProfile.setLanguagesList(languageObjects);
    } catch (IOException e) {
      System.out.println(GET_REQUEST_IOERROR);
    }
  }

  public GiTinderProfile fetchProfileFromGitHub(String accessToken, String username) {
    GitHubClient gitHubClient = setUpGitHubClient(accessToken);
    setLoginAndAvatar(gitHubClient, username);
    setEverythingElse(gitHubClient, username);
    return giTinderProfile;
  }

  public ResponseEntity<?> getOtherProfile(String appToken, String username) {
    if (appToken == null || userRepository.findByAppToken(appToken) == null) {
      return errorService.unauthorizedRequestError();
    }
    if (userRepository.findByUserName(username) == null) {
      return errorService.noSuchUserError();
    }
    GiTinderUser authenticatedUser = userRepository.findByUserNameAndAppToken(username, appToken);
    if (profileRepository.findByLogin(authenticatedUser.getUserName()) == null || refreshRequired(
        profileRepository
            .findByLogin(authenticatedUser.getUserName()))) {
      profileRepository.save(fetchProfileFromGitHub(authenticatedUser.getAccessToken(), username));
    }
    GiTinderProfile upToDateProfile = profileRepository
        .findByLogin(authenticatedUser.getUserName());
    return new ResponseEntity<Object>(upToDateProfile, HttpStatus.OK);
  }

  public ResponseEntity<?> getOwnProfile(String appToken) {
    if (!appToken.equals("")) {
      GiTinderProfile mockProfile = mockProfileBuilder.build();
      return new ResponseEntity<Object>(mockProfile, HttpStatus.OK);
    }
    return errorService.unauthorizedRequestError();
  }

  public int daysPassedSinceLastRefresh(GiTinderProfile profileToCheck) {
    Timestamp currentDate = new Timestamp(System.currentTimeMillis());
    Timestamp lastRefresh = profileToCheck.getRefreshDate();
    long differenceAsLong = currentDate.getTime() - lastRefresh.getTime();
    int differenceAsDays = (int) (differenceAsLong / (1000 * 60 * 60 * 24));
    return differenceAsDays;
  }

  public boolean refreshRequired(GiTinderProfile profileToCheck) {
    if (daysPassedSinceLastRefresh(profileToCheck) >= 1) {
      return true;
    }
    return false;
  }
}
