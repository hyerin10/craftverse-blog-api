package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.model.dto.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.service.AuthService;
import kr.co.craftverse.craftverse_blog_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final AuthService authService;

  @PostMapping("/register")
  public RestResult<Map<String, Object>> registerUser(@Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    UserResponseDTO userResponseDTO = userService.registerUser(userRegistrationRequestDTO);
    data.put("user", userResponseDTO);
    return new RestResult<>(data);
  }

  @PostMapping("/login")
  public RestResult<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    String accessToken = authService.login(loginRequestDTO);
    data.put("accessToken", accessToken);
    return new RestResult<>(data);
  }
}
