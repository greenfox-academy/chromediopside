package com.chromediopside.controller;

import com.chromediopside.datatransfer.MessageDTO;
import com.chromediopside.datatransfer.MessageStatusOK;
import com.chromediopside.datatransfer.Messages;
import com.chromediopside.datatransfer.StatusResponseOK;
import com.chromediopside.model.Message;
import com.chromediopside.service.ErrorService;
import com.chromediopside.service.GiTinderUserService;
import com.chromediopside.service.MatchService;
import com.chromediopside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

  private MessageService messageService;
  private GiTinderUserService userService;
  private ErrorService errorService;
  private MatchService matchService;

  @Autowired
  public MessageController(MessageService messageService,
      GiTinderUserService userService,
      ErrorService errorService,
      MatchService matchService) {
    this.messageService = messageService;
    this.userService = userService;
    this.errorService = errorService;
    this.matchService = matchService;
  }

  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<Object> exception(ServletRequestBindingException ex) {
    return new ResponseEntity<>(errorService.unauthorizedRequestError(), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Object> exception(HttpMessageNotReadableException ex) {
    return new ResponseEntity<>(errorService.missingRequestBodyError(), HttpStatus.BAD_REQUEST);
  }

  @GetMapping(value = "/messages/{username}")
  public ResponseEntity<Object> getMessages(
          @RequestHeader(name = "X-GiTinder-token") String appToken,
          @PathVariable String username) {
    if (!userService.validAppToken(appToken)) {
      return new ResponseEntity<>(errorService.unauthorizedRequestError(), HttpStatus.UNAUTHORIZED);
    }
    Messages conversation = messageService.getConversationBetweenUsers(appToken, username);
    return new ResponseEntity<>(conversation, HttpStatus.OK);
  }

  @PostMapping(value = "/messages")
  public ResponseEntity<Object> postMessage(
          @RequestHeader(name = "X-GiTinder-token") String appToken,
          @RequestBody MessageDTO messageDTO) throws Exception {
    if (!userService.validAppToken(appToken) || !matchService.areTheyMatched(appToken, messageDTO)) {
      return new ResponseEntity<>(errorService.unauthorizedRequestError(), HttpStatus.UNAUTHORIZED);
    }
    Message message = messageService.postMessage(messageDTO, appToken);
    MessageStatusOK messageStatusOK = new MessageStatusOK(message);
    return new ResponseEntity(messageStatusOK, HttpStatus.OK);
  }

  @DeleteMapping(value = "/messages/{id}")
  public ResponseEntity<Object> deleteMessage(
          @RequestHeader(name = "X-GiTinder-token") String appToken,
          @PathVariable long id) {
    if (!userService.validAppToken(appToken) || !messageService.isOwnMessage(appToken, id)) {
      return new ResponseEntity<>(errorService.unauthorizedRequestError(), HttpStatus.UNAUTHORIZED);
    }
    messageService.deleteMessage(id);
    StatusResponseOK ok = new StatusResponseOK();
    return new ResponseEntity<>(ok, HttpStatus.OK);
  }
}
