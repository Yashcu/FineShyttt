package com.yash.fineshyttt.service.cart;

import com.yash.fineshyttt.domain.*;
import com.yash.fineshyttt.exception.ResourceNotFoundException;
import com.yash.fineshyttt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Get or create cart for user
     */
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Add item to cart or update quantity if exists
     */
    public Cart addItem(User user, Long variantId, int quantity) {
        // Validate variant exists and has stock
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product variant not found")
                );

        Inventory inventory = inventoryRepository.findByVariant_Id(variantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inventory not found")
                );

        if (inventory.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Only " + inventory.getQuantity() + " items available"
            );
        }

        Cart cart = getOrCreateCart(user);

        // Check if item already in cart
        CartItem existingItem = cart.getItems()
                .stream()
                .filter(item -> item.getVariant().getId().equals(variantId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Update quantity
            int newQuantity = existingItem.getQuantity() + quantity;

            if (inventory.getQuantity() < newQuantity) {
                throw new IllegalArgumentException(
                        "Cannot add more items. Only " +
                                inventory.getQuantity() + " available"
                );
            }

            existingItem.setQuantity(newQuantity);
        } else {
            // Add new item
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(quantity)
                    .build();

            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        return cartRepository.save(cart);
    }

    /**
     * Update item quantity
     */
    public Cart updateItemQuantity(User user, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems()
                .stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item not found")
                );

        // Validate stock
        Inventory inventory = inventoryRepository
                .findByVariant_Id(item.getVariant().getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inventory not found")
                );

        if (inventory.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Only " + inventory.getQuantity() + " items available"
            );
        }

        item.setQuantity(quantity);
        return cartRepository.save(cart);
    }

    /**
     * Remove item from cart
     */
    public Cart removeItem(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems()
                .stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item not found")
                );

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return cartRepository.save(cart);
    }

    /**
     * Clear entire cart
     */
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    /**
     * Get cart total
     */
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(User user) {
        Cart cart = getOrCreateCart(user);

        return cart.getItems()
                .stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
