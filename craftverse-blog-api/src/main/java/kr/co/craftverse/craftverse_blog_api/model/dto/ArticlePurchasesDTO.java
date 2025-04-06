package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticlePurchasesDTO {
  @NotNull
  private Long id;
  private Long purchaseDate;
  private BigDecimal purchasePrice;
  private String paymentStatus;
  @NotNull
  private String title;
  @NotNull
  private String content;
  @Pattern(regexp = "overoll|tech|series")
  private String category;
  @Pattern(regexp = "ko|en")
  private String language;
  private Boolean isPremium;
  private Long createdAt;
  private Long updatedAt;
  private Integer viewsCount;
  private String slug;
  private String metaDescription;
}
