package kr.co.craftverse.craftverse_blog_api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name="article_purchases")
public class ArticlePurchases {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name="user_id")
  private Long userId;
  @Column(name="article_id_ko")
  private Long articleIdKo;
  @Column(name="article_id_en")
  private Long articleIdEn;
  @Column(name="purchase_date")
  private Long purchaseDate;
  @Column(name="purchase_price")
  private BigDecimal purchasePrice;
  @Column(name="payment_status")
  private String paymentStatus;
}
