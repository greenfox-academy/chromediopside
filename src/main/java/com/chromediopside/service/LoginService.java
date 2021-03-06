package com.chromediopside.service;

import com.chromediopside.datatransfer.LoginForm;
import com.chromediopside.datatransfer.TokenResponse;
import java.io.IOException;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

  private GiTinderUserService userService;
  private LogService logService;

  @Autowired
  public LoginService(GiTinderUserService userService, LogService logService) {
    this.userService = userService;
    this.logService = logService;
  }

  public TokenResponse login(LoginForm loginForm) {
    String appToken = userService.createAndSaveUser(loginForm);
    TokenResponse tokenResponse = new TokenResponse(appToken);
    return tokenResponse;
  }

  public boolean loginFormContainsValidAccessToken(LoginForm loginForm) {
    if (loginForm.getAccessToken() == null || loginForm.getUsername() == null || loginForm == null) {
      return false;
    }
    GitHubClient gitHubClient =  GitHubClientService.setUpGitHubClient(loginForm.getAccessToken());
    UserService userService = new UserService(gitHubClient);
    try {
      User user = userService.getUser();
      if (user.getLogin().equals(loginForm.getUsername())) {
        return true;
      }
    } catch (IOException e) {
      logService.printLogMessage("ERROR", GitHubClientService.getGetRequestIoerror());
    }
    return false;
  }
}
