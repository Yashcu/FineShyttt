package com.yash.fineshyttt.controller.cart;

import com.yash.fineshyttt.config.ApiConstants;
import com.yash.fineshyttt.domain.Cart;
import com.yash.fineshyttt.dto.cart.CartItemRequest;
import com.yash.fineshyttt.dto.cart.CartResponse;
import com.yash.fineshyttt.security.UserPrincipal;
import com.yash.fineshyttt.service.cart.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.CART_BASE)
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Cart cart = cartService.getOrCreateCart(principal.getUser());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PostMapping(ApiConstants.CART_ITEMS)
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CartItemRequest request
    ) {
        Cart cart = cartService.addItem(
                principal.getUser(),
                request.variantId(),
                request.quantity()
        );
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PutMapping(ApiConstants.CART_ITEM_BY_VARIANT)
    public ResponseEntity<CartResponse> updateItemQuantity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId,
            @RequestParam int quantity
    ) {
        Cart cart = cartService.updateItemQuantity(
                principal.getUser(),
                itemId,
                quantity
        );
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping(ApiConstants.CART_ITEM_BY_VARIANT)
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId
    ) {
        Cart cart = cartService.removeItem(
                principal.getUser(),
                itemId
        );
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        cartService.clearCart(principal.getUser());
        return ResponseEntity.noContent().build();
    }
}
