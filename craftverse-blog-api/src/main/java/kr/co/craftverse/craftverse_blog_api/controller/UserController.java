package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
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

  @PostMapping("/register")
  public RestResult<Map<String, Object>> registerUser(@Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    UserResponseDTO userResponseDTO = userService.registerUser(userRegistrationRequestDTO);
    data.put("user", userResponseDTO);
    return new RestResult<>(data);
  }
}
