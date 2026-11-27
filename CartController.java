package com.fooddelivery.controller;

import com.fooddelivery.dto.request.CartItemRequest;
import com.fooddelivery.dto.response.*;
import com.fooddelivery.security.JwtUtil;
import com.fooddelivery.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "Get current user cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(ApiResponse.ok("Cart fetched", cartService.getCart(uid(auth))));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader("Authorization") String auth,
            @Valid @RequestBody CartItemRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Item added", cartService.addItem(uid(auth), req)));
    }

    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Update item quantity (0 = remove)")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long cartItemId,
            @RequestParam @Min(0) @Max(20) Integer quantity) {
        return ResponseEntity.ok(ApiResponse.ok("Cart updated",
                cartService.updateItem(uid(auth), cartItemId, quantity)));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(ApiResponse.ok("Item removed",
                cartService.removeItem(uid(auth), cartItemId)));
    }

    @DeleteMapping
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader("Authorization") String auth) {
        cartService.clearCart(uid(auth));
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared"));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
