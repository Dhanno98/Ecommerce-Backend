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
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
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
        Cart cart = findCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(
                cart.getCartId(),
                productId
        );
        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }
        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is out of stock");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName() +
                    " less than or equal to the stock quantity " + product.getQuantity() + ".");
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setCart(cart);
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cart.getCartItems().add(newCartItem);
        cartRepository.save(cart);

        return mapToCartDTO(cart);
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if (carts.isEmpty()) {
            throw new APIException("No cart exist");
        }

        List<CartDTO> cartDTOS = carts.stream()
                .map(this::mapToCartDTO)
                .toList();

        return cartDTOS;
    }

    @Override
    public CartDTO getCart() {
        Long userId = authUtil.loggedInUserId();
        Cart cart = cartRepository.findCartByUserId(userId);

        if (cart == null) {
            throw new APIException("Cart not yet created!");
        }

        return mapToCartDTO(cart);
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, String operation) {
        if (!ALLOWED_OPERATIONS.contains(operation.toLowerCase())) {
            throw new APIException("Invalid operation. Supported operations: add, delete");
        }

        Integer op = operation.equalsIgnoreCase("delete") ? -1 : 1;

        Long userId = authUtil.loggedInUserId();
        Cart cart = cartRepository.findCartByUserId(userId);
        if (cart == null) {
            throw new APIException("Cart not yet created!");
        }
        Long cartId = cart.getCartId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!");
        }

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is out of stock");
        }

        if (product.getQuantity() < cartItem.getQuantity() + op) {
            throw new APIException("Please, make an order of the " + product.getProductName() +
                    " less than or equal to the stock quantity " + product.getQuantity() + ".");
        }

        int newQuantity = cartItem.getQuantity() + op;
        if (newQuantity < 0) {
            throw new APIException("The resulting quantity cannot be negative");
        }
        if (newQuantity == 0) {
            deleteProductFromCart(productId);
        } else {
            cartItem.setQuantity(newQuantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * op));
            cartRepository.save(cart);
        }

        return mapToCartDTO(cart);
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long productId) {
        Long userId = authUtil.loggedInUserId();
        Cart cart = cartRepository.findCartByUserId(userId);
        if (cart == null) {
            throw new APIException("Cart not yet created!");
        }

        Long cartId = cart.getCartId();

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItem == null) {
            throw new ResourceNotFoundException("CartItem", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));
        cart.getCartItems().remove(cartItem);

        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart!";
    }

    @Transactional
    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> cartItemDTOS) {
        // Get user's email
        String emailId= authUtil.loggedInEmail();

        // Check if an existing cart is available or create a new one
        Cart existingCart = cartRepository.findCartByEmail(emailId);
        if (existingCart == null) {
            existingCart = new Cart();
            existingCart.setTotalPrice(0.00);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart = cartRepository.save(existingCart);
        } else {
            // Clear all current items in the existing cart
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }

        double totalPrice = 0.00;

        // Process each item in the request to add to the cart
        for (CartItemDTO cartItemDTO : cartItemDTOS) {
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            // Find product by ID
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            // Directly update product stock and total price
            // product.setQuantity(product.getQuantity() - quantity);
            totalPrice += product.getSpecialPrice() * quantity;

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
        return "Cart created/updated with new items successfully";
    }

    private Cart findCart() {
        Cart userCart = cartRepository.findCartByUserId(authUtil.loggedInUserId());
        if (userCart != null) {
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.0);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart = cartRepository.save(cart);
        return newCart;
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
