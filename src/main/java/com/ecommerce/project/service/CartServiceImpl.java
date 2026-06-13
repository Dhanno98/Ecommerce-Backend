package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import com.ecommerce.project.payload.CartItemResponseDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import com.ecommerce.project.util.ImageUrlUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    private final ProductRepository productRepository;

    private final CartItemRepository cartItemRepository;

    private final AuthUtil authUtil;

    private final ModelMapper modelMapper;

    private final ImageUrlUtil imageUrlUtil;

    private static final Set<String> ALLOWED_OPERATIONS = Set.of("add", "delete");

    @Transactional
    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        log.info("Add to cart requested. userId={}, productId={}, quantity={}",
                authUtil.loggedInUserId(), productId, quantity);

        Cart cart = findCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItem != null) {
            log.warn("Product already exists in cart. userId={}, productId={}", authUtil.loggedInUserId(), productId);
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }
        if (product.getQuantity() == 0) {
            log.warn("Out of stock while adding to cart. productId={}", productId);
            throw new APIException(product.getProductName() + " is out of stock");
        }
        if (product.getQuantity() < quantity) {
            log.warn("Insufficient stock. productId={}, requested={}, available={}",
                    productId, quantity, product.getQuantity());
            throw new APIException("Please, make an order of the " + product.getProductName() +
                    " less than or equal to the stock quantity " + product.getQuantity() + ".");
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setCart(cart);
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cart.getTotalPrice().add((product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)))));
        cart.getCartItems().add(newCartItem);
        cartRepository.save(cart);

        log.info("Product added to cart successfully. userId={}, productId={}, quantity={}",
                authUtil.loggedInUserId(), productId, quantity);
        return mapToCartDTO(cart);
    }

    @Override
    public List<CartDTO> getAllCarts() {
        log.debug("Fetching all carts");
        List<Cart> carts = cartRepository.findAll();

        if (carts.isEmpty()) {
            log.warn("No carts found");
            throw new APIException("No cart exist");
        }

        return carts.stream()
                .map(this::mapToCartDTO)
                .toList();
    }

    @Override
    public CartDTO getCart() {
        Long userId = authUtil.loggedInUserId();
        Cart cart = cartRepository.findCartByUserId(userId);

        if (cart == null) {
            log.warn("Cart not found for userId={}", userId);
            throw new APIException("Cart not yet created!");
        }

        return mapToCartDTO(cart);
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, String operation) {
        Long userId = authUtil.loggedInUserId();

        log.info("Cart quantity update requested. userId={}, productId={}, operation={}",
                userId, productId, operation);

        if (!ALLOWED_OPERATIONS.contains(operation.toLowerCase())) {
            log.warn("Invalid cart operation. userId={}, operation={}", userId, operation);
            throw new APIException("Invalid operation. Supported operations: add, delete");
        }

        Integer op = operation.equalsIgnoreCase("delete") ? -1 : 1;

        Cart cart = cartRepository.findCartByUserId(userId);
        if (cart == null) {
            log.warn("Cart not found for userId={} in update cart.", userId);
            throw new APIException("Cart not yet created!");
        }
        Long cartId = cart.getCartId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItem == null) {
            log.warn("Product not in cart. cartId={}, productId={}", cartId, productId);
            throw new APIException("Product " + product.getProductName() + " not available in the cart!");
        }

        if (product.getQuantity() == 0) {
            log.warn("Out of stock while updating cart. productId={}", productId);
            throw new APIException(product.getProductName() + " is out of stock");
        }

        if (product.getQuantity() < cartItem.getQuantity() + op) {
            log.warn("Insufficient stock during cart update. productId={}, requested={}, available={}",
                    productId, cartItem.getQuantity() + op, product.getQuantity());
            throw new APIException("Please, make an order of the " + product.getProductName() +
                    " less than or equal to the stock quantity " + product.getQuantity() + ".");
        }

        int newQuantity = cartItem.getQuantity() + op;
        if (newQuantity < 0) {
            log.warn("Negative quantity prevented. userId={}, productId={}, currentQuantity={}",
                    userId, productId, cartItem.getQuantity());
            throw new APIException("The resulting quantity cannot be negative");
        }
        if (newQuantity == 0) {
            log.info("Cart item quantity reached zero. userId={}, productId={}", userId, productId);
            deleteProductFromCart(productId);
        } else {
            cartItem.setQuantity(newQuantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice().add((cartItem.getProductPrice().multiply(BigDecimal.valueOf(op)))));
            cartRepository.save(cart);
            log.info("Cart quantity updated successfully. userId={}, productId={}, newQuantity={}",
                    userId, productId, newQuantity);
        }

        return mapToCartDTO(cart);
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long productId) {
        Long userId = authUtil.loggedInUserId();
        log.info("Cart item deletion requested. userId={}, productId={}", userId, productId);

        Cart cart = cartRepository.findCartByUserId(userId);
        if (cart == null) {
            log.warn("Cart not found for userId={} in delete cart.", userId);
            throw new APIException("Cart not yet created!");
        }

        Long cartId = cart.getCartId();

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItem == null) {
            log.warn("Cart item not found. cartId={}, productId={}", cartId, productId);
            throw new ResourceNotFoundException("CartItem", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice()
                .subtract(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))));
        cart.getCartItems().remove(cartItem);

        log.info("Cart item deleted successfully. userId={}, productId={}", userId, productId);
        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart!";
    }

    @Transactional
    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> cartItemDTOS) {
        // Get user's email
        String emailId= authUtil.loggedInEmail();

        log.info("Bulk cart update requested. userEmail={}, itemCount={}", emailId, cartItemDTOS.size());

        // Check if an existing cart is available or create a new one
        Cart existingCart = cartRepository.findCartByEmail(emailId);
        if (existingCart == null) {
            log.info("Creating new cart for userEmail={}", emailId);
            existingCart = new Cart();
            existingCart.setTotalPrice(BigDecimal.ZERO);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart = cartRepository.save(existingCart);
        } else {
            // Clear all current items in the existing cart
            log.info("Replacing existing cart contents. cartId={}", existingCart.getCartId());
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        // Process each item in the request to add to the cart
        for (CartItemDTO cartItemDTO : cartItemDTOS) {
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            // Find product by ID
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            // Directly update product stock and total price
            // product.setQuantity(product.getQuantity() - quantity);
            totalPrice = totalPrice.add(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)));

            // Create and save cart item
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        }

        // Update the cart's total price and save
        existingCart.setTotalPrice(totalPrice);
        cartRepository.save(existingCart);

        log.info("Cart updated successfully. cartId={}, itemCount={}, totalPrice={}",
                existingCart.getCartId(), cartItemDTOS.size(), totalPrice);
        return "Cart created/updated with new items successfully";
    }

    private Cart findCart() {
        Cart userCart = cartRepository.findCartByUserId(authUtil.loggedInUserId());
        if (userCart != null) {
            return userCart;
        }

        log.info("Creating new cart for userId={}", authUtil.loggedInUserId());

        Cart cart = new Cart();
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }

    private CartItemResponseDTO mapToCartItemResponseDTO(CartItem item) {
        CartItemResponseDTO responseDTO = modelMapper.map(item.getProduct(), CartItemResponseDTO.class);
        responseDTO.setQuantity(item.getQuantity());
        responseDTO.setImage(imageUrlUtil.constructImageUrl(item.getProduct().getImage()));
        return responseDTO;
    }

    private CartDTO mapToCartDTO(Cart cart) {
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItemResponseDTO> cartItemResponseDTOS = cart.getCartItems()
                .stream().map(this::mapToCartItemResponseDTO)
                .toList();
        cartDTO.setCartItems(cartItemResponseDTOS);
        return cartDTO;
    }
}
