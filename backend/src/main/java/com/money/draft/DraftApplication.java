package com.money.draft;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.entity.RewardItem;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.domain.repository.RewardItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication
public class DraftApplication {

    private static final Logger log = LoggerFactory.getLogger(DraftApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DraftApplication.class, args);
    }

    @Bean
    public CommandLineRunner migratePasswords(AppUserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            for (AppUser user : userRepo.findAll()) {
                if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
                    log.info("Re-encoding plaintext password for user: {}", user.getUsername());
                    user.setPassword(encoder.encode(user.getPassword()));
                    userRepo.save(user);
                }
            }
        };
    }

    @Bean
    public CommandLineRunner seedRewardItems(RewardItemRepository itemRepo) {
        return args -> {
            if (itemRepo.count() > 0) {
                log.info("Reward items already seeded, skipping");
                return;
            }
            List<RewardItem> items = List.of(
                new RewardItem("Amazon Gift Card", "Amazon", 500, new BigDecimal("250.00")),
                new RewardItem("Swiggy Food Voucher", "Swiggy", 200, new BigDecimal("100.00")),
                new RewardItem("Myntra Fashion Coupon", "Myntra", 350, new BigDecimal("175.00")),
                new RewardItem("Uber Ride Discount", "Uber", 250, new BigDecimal("125.00")),
                new RewardItem("Zomato Dining Voucher", "Zomato", 150, new BigDecimal("75.00"))
            );
            String[] images = {
                "https://placehold.co/300x200/FF9900/white?text=Amazon",
                "https://placehold.co/300x200/FC8019/white?text=Swiggy",
                "https://placehold.co/300x200/E91E63/white?text=Myntra",
                "https://placehold.co/300x200/000000/white?text=Uber",
                "https://placehold.co/300x200/E23744/white?text=Zomato"
            };
            for (int i = 0; i < items.size(); i++) {
                RewardItem item = items.get(i);
                item.setDescription("Discount coupon for " + item.getBrand());
                item.setImageUrl(images[i]);
                itemRepo.save(item);
                log.info("Seeded reward item: {} [{}]", item.getName(), item.getImageUrl());
            }
        };
    }
}
