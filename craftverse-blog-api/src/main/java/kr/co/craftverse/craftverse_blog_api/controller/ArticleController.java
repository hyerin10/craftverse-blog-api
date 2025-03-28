package kr.co.craftverse.craftverse_blog_api.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("articles")
public class ArticleController {
  private final ArticleService articleService;

  @GetMapping
  public RestResult getAll() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("articles", articleService.getAll());
    return new RestResult(data);
  }
}
