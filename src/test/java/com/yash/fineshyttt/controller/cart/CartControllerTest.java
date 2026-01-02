package com.yash.fineshyttt.controller.cart;

import com.yash.fineshyttt.BaseIntegrationTest;
import com.yash.fineshyttt.domain.*;
import com.yash.fineshyttt.dto.cart.CartItemRequest;
import com.yash.fineshyttt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private ProductVariant testVariant;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .email("cart@example.com")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .build();
        userRepository.save(testUser);

        // Create test product
        Category category = Category.builder()
                .name("Test Category")
                .isActive(true)
                .build();
        categoryRepository.save(category);

        Product product = Product.builder()
                .name("Test Product")
                .slug("test-product")
                .description("Test Description")
                .category(category)
                .isActive(true)
                .build();
        productRepository.save(product);

        testVariant = ProductVariant.builder()
                .product(product)
                .sku("TEST-SKU-001")
                .material("Cotton")
                .color("Blue")
                .size("M")
                .price(BigDecimal.valueOf(1000))
                .isActive(true)
                .build();
        variantRepository.save(testVariant);

        // Add inventory
        Inventory inventory = Inventory.builder()
                .variant(testVariant)
                .quantity(100)
                .reservedQuantity(0)
                .build();
        inventoryRepository.save(inventory);
    }

    @Test
    @WithMockUser(username = "cart@example.com")
    void shouldAddItemToCart() throws Exception {
        CartItemRequest request = new CartItemRequest(
                testVariant.getId(),
                2
        );

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    @WithMockUser(username = "cart@example.com")
    void shouldGetEmptyCart() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void shouldRejectUnauthenticatedCartAccess() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }
}
