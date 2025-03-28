package kr.co.craftverse.craftverse_blog_api.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArticleController {
  private final ArticleService articleService;

  @GetMapping("/articles")
  public RestResult getAll() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("articles", articleService.getAll());
    return new RestResult(data);
  }

  @GetMapping("/article/{id}")
  public RestResult getById(@PathVariable long id) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("article", articleService.getById(id));
    return new RestResult(data);
  }

  @PostMapping("/article")
  public RestResult create(@RequestBody ArticleDTO articleDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("article", articleService.create(articleDTO));
    return new RestResult(data);
  }

  @PatchMapping("/article/{id}")
  public RestResult update(@PathVariable("id") long id, @RequestBody ArticleDTO articleDTO)
      throws Exception {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("article", articleService.update(id, articleDTO));
    return new RestResult(data);
  }

  @DeleteMapping("/article/{id}")
  public RestResult delete(@PathVariable("id") long id) throws Exception {
    Map<String, Object> data = new LinkedHashMap<>();
    articleService.delete(id);
    data.put("success", "true");
    return new RestResult(data);
  }
}
