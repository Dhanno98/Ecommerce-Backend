package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemResponseDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import com.ecommerce.project.util.ImageUrlUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartServiceImplTest {

    @InjectMocks
    CartServiceImpl cartService;

    @Mock
    AuthUtil authUtil;

    @Mock
    ProductRepository productRepository;

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    CartRepository cartRepository;

    @Mock
    ModelMapper modelMapper;

    @Mock
    ImageUrlUtil imageUrlUtil;

    /// addProductToCart()
    @Test
    void addProductToCartShouldAddProductToCartWhenUserAlreadyHasACart() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        int quantity = 3;

        Cart userCart = createCart(user);
        userCart.setTotalPrice(BigDecimal.ZERO);

        CartDTO cartDTO = createCartDTO(userCart);
        cartDTO.setTotalPrice(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)));

        CartItemResponseDTO cartItemResponseDTO = createCartItemResponseDTO(product);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(userCart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(userCart.getCartId(), product.getProductId()))
                .thenReturn(null);

        when(cartRepository.save(userCart))
                .thenReturn(userCart);

        when(modelMapper.map(product, CartItemResponseDTO.class))
                        .thenReturn(cartItemResponseDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn("http://localhost/images/" + product.getImage());

        when(modelMapper.map(userCart, CartDTO.class))
                .thenReturn(cartDTO);

        CartDTO result = cartService.addProductToCart(1L, quantity);

        assertNotNull(result);
        assertEquals(1L, result.getCartId());
        assertEquals(1, result.getCartItems().size());
        assertEquals(0, result.getTotalPrice().compareTo(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity))));

        CartItemResponseDTO item = result.getCartItems().getFirst();

        assertEquals(product.getProductId(), item.getProductId());
        assertEquals(product.getProductName(), item.getProductName());
        assertEquals(quantity, item.getQuantity());
        assertEquals(product.getSpecialPrice(), item.getSpecialPrice());
        assertEquals(product.getDiscount(), item.getDiscount());

        assertEquals(1, userCart.getCartItems().size());

        CartItem cartItem = userCart.getCartItems().getFirst();

        assertEquals(quantity, cartItem.getQuantity());
        assertEquals(product.getProductId(), cartItem.getProduct().getProductId());
        assertEquals(product.getDiscount(), cartItem.getDiscount());
        assertEquals(product.getSpecialPrice(), cartItem.getProductPrice());

        assertEquals(0, product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)).compareTo(userCart.getTotalPrice()));

        verify(authUtil, atLeastOnce()).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(userCart.getCartId(), product.getProductId());
        verify(cartRepository).save(userCart);
        verify(modelMapper).map(product, CartItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
        verify(modelMapper).map(userCart, CartDTO.class);
        verify(authUtil, never()).loggedInUser();
    }

    @Test
    void addProductToCartShouldCreateACartAndAddProductToCartWhenUserHasNoCart() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        int quantity = 3;

        CartDTO cartDTO = new CartDTO();
        cartDTO.setCartId(1L);
        cartDTO.setTotalPrice(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)));

        CartItemResponseDTO cartItemResponseDTO = createCartItemResponseDTO(product);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(null);

        when(authUtil.loggedInUser())
                .thenReturn(user);

        List<BigDecimal> capturedTotals = new ArrayList<>();
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> {
                    Cart cart = invocation.getArgument(0);

                    capturedTotals.add(cart.getTotalPrice());

                    if (cart.getCartId() == null) {
                        cart.setCartId(1L);
                    }

                    return cart;
                });

        when(productRepository.findById(product.getProductId()))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(any(Long.class), eq(product.getProductId())))
                .thenReturn(null);

        when(modelMapper.map(any(Product.class), eq(CartItemResponseDTO.class)))
                .thenReturn(cartItemResponseDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn("http://localhost/images/" + product.getImage());

        when(modelMapper.map(any(Cart.class), eq(CartDTO.class)))
                .thenReturn(cartDTO);

        CartDTO result = cartService.addProductToCart(1L, quantity);

        assertNotNull(result);
        assertEquals(1L, result.getCartId());
        assertEquals(1, result.getCartItems().size());
        assertEquals(0, result.getTotalPrice().compareTo(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity))));

        CartItemResponseDTO item = result.getCartItems().getFirst();

        assertEquals(product.getProductId(), item.getProductId());
        assertEquals(product.getProductName(), item.getProductName());
        assertEquals(quantity, item.getQuantity());
        assertEquals(product.getSpecialPrice(), item.getSpecialPrice());
        assertEquals(product.getDiscount(), item.getDiscount());

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, times(2)).save(cartCaptor.capture());
        List<Cart> savedCarts = cartCaptor.getAllValues();
        Cart finalSavedCart = savedCarts.get(1);

        assertEquals(BigDecimal.ZERO, capturedTotals.get(0));
        assertEquals(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)), capturedTotals.get(1));

        assertEquals(1, finalSavedCart.getCartItems().size());

        CartItem cartItem = finalSavedCart.getCartItems().getFirst();

        assertEquals(quantity, cartItem.getQuantity());
        assertEquals(product.getProductId(), cartItem.getProduct().getProductId());
        assertEquals(product.getDiscount(), cartItem.getDiscount());
        assertEquals(product.getSpecialPrice(), cartItem.getProductPrice());

        assertEquals(0, product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)).compareTo(finalSavedCart.getTotalPrice()));

        verify(authUtil, atLeastOnce()).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(any(Long.class), eq(product.getProductId()));
        verify(modelMapper).map(product, CartItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
        verify(modelMapper).map(any(Cart.class), eq(CartDTO.class));
    }

    @Test
    void addProductToCartShouldThrowApiExceptionWhenRequestedQuantityIsZero() {
        int quantity = 0;

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.addProductToCart(1L, quantity)
        );

        assertEquals("Requested quantity must be greater than 0", exception.getMessage());

        verify(productRepository, never()).findById(1L);
    }

    @Test
    void addProductToCartShouldThrowApiExceptionWhenRequestedQuantityIsNegative() {
        int quantity = -1;

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.addProductToCart(1L, quantity)
        );

        assertEquals("Requested quantity must be greater than 0", exception.getMessage());

        verify(productRepository, never()).findById(1L);
    }

    @Test
    void addProductToCartShouldThrowResourceNotFoundExceptionWhenProductDoesNotExist() {
        User user = createUser(1L);

        Cart userCart = createCart(user);
        userCart.setTotalPrice(BigDecimal.ZERO);

        int quantity = 3;

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(userCart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addProductToCart(1L, quantity)
        );

        assertEquals("Product not found with productId: 1", exception.getMessage());

        verify(authUtil, atLeastOnce()).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(authUtil, never()).loggedInUser();
        verify(cartItemRepository, never()).findCartItemByProductIdAndCartId(userCart.getCartId(), 1L);
    }

    @Test
    void addProductToCartShouldThrowApiExceptionWhenProductAlreadyExistsInTheCart() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart userCart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(userCart);

        userCart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        userCart.setCartItems(new ArrayList<>(List.of(cartItem)));

        int quantity = 3;

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(userCart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(userCart.getCartId(), 1L))
                .thenReturn(cartItem);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.addProductToCart(1L, quantity)
        );

        assertEquals("Product " + product.getProductName() + " already exists in the cart", exception.getMessage());

        verify(authUtil, atLeastOnce()).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(userCart.getCartId(), 1L);
        verify(authUtil, never()).loggedInUser();
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addProductToCartShouldThrowApiExceptionWhenProductIsOutOfStock() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);
        product.setQuantity(0);

        Cart userCart = createCart(user);
        userCart.setTotalPrice(BigDecimal.ZERO);

        int quantity = 3;

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(userCart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(userCart.getCartId(), 1L))
                .thenReturn(null);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.addProductToCart(1L, quantity)
        );

        assertEquals(product.getProductName() + " is out of stock", exception.getMessage());

        verify(authUtil, atLeastOnce()).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(userCart.getCartId(), 1L);
        verify(authUtil, never()).loggedInUser();
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addProductToCartShouldThrowApiExceptionWhenRequestedQuantityIsGreaterThanStockProductQuantity() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);
        product.setQuantity(10);

        Cart userCart = createCart(user);
        userCart.setTotalPrice(BigDecimal.ZERO);

        int quantity = 11;

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(userCart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(userCart.getCartId(), 1L))
                .thenReturn(null);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.addProductToCart(1L, quantity)
        );

        assertEquals("Please, make an order of the " + product.getProductName() +
                " less than or equal to the stock quantity " + product.getQuantity() + ".", exception.getMessage());

        verify(authUtil, atLeastOnce()).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(userCart.getCartId(), 1L);
        verify(authUtil, never()).loggedInUser();
        verify(cartRepository, never()).save(any(Cart.class));
    }

    /// getAllCarts()
    @Test
    void getAllCartsShouldReturnAllCartsWithoutItems() {
        User user = createUser(1L);

        Cart cart = createCart(user);
        cart.setTotalPrice(BigDecimal.ZERO);

        List<Cart> carts = List.of(cart);

        CartDTO cartDTO = createCartDTO(cart);

        when(cartRepository.findAll())
                .thenReturn(carts);

        when(modelMapper.map(cart, CartDTO.class))
                .thenReturn(cartDTO);

        List<CartDTO> result = cartService.getAllCarts();

        assertNotNull(result);
        assertEquals(1, result.size());

        CartDTO returnedCart = result.getFirst();

        assertEquals(cart.getCartId(), returnedCart.getCartId());
        assertEquals(cart.getTotalPrice(), returnedCart.getTotalPrice());

        verify(cartRepository).findAll();
        verify(modelMapper).map(cart, CartDTO.class);
        verify(modelMapper, never()).map(any(Product.class), eq(CartItemResponseDTO.class));
    }

    @Test
    void getAllCartsShouldReturnAllCartsWithItems() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);

        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        List<Cart> carts = List.of(cart);

        CartDTO cartDTO = createCartDTO(cart);

        CartItemResponseDTO cartItemResponseDTO = createCartItemResponseDTO(product);

        when(cartRepository.findAll())
                .thenReturn(carts);

        when(modelMapper.map(cart, CartDTO.class))
                .thenReturn(cartDTO);

        when(modelMapper.map(product, CartItemResponseDTO.class))
                .thenReturn(cartItemResponseDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn("http://localhost/images/" + product.getImage());

        List<CartDTO> result = cartService.getAllCarts();

        assertNotNull(result);
        assertEquals(1, result.size());

        CartDTO returnedCart = result.getFirst();

        assertEquals(cart.getCartId(), returnedCart.getCartId());
        assertEquals(cart.getTotalPrice(), returnedCart.getTotalPrice());

        assertNotNull(returnedCart.getCartItems());
        assertEquals(1, returnedCart.getCartItems().size());

        CartItemResponseDTO returnedItem = returnedCart.getCartItems().getFirst();

        assertEquals(product.getProductId(), returnedItem.getProductId());
        assertEquals(product.getProductName(), returnedItem.getProductName());
        assertEquals(cartItem.getQuantity(), returnedItem.getQuantity());
        assertEquals(product.getPrice(), returnedItem.getPrice());
        assertEquals(product.getDiscount(), returnedItem.getDiscount());
        assertEquals(product.getSpecialPrice(), returnedItem.getSpecialPrice());
        assertEquals("http://localhost/images/" + product.getImage(), returnedItem.getImage());

        verify(cartRepository).findAll();
        verify(modelMapper).map(cart, CartDTO.class);
        verify(modelMapper).map(product, CartItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
    }

    @Test
    void getAllCartsShouldThrowApiExceptionWhenNoCartExists() {
        List<Cart> carts = new ArrayList<>();

        when(cartRepository.findAll())
                .thenReturn(carts);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.getAllCarts()
        );

        assertEquals("No cart exist", exception.getMessage());

        verify(cartRepository).findAll();
        verify(modelMapper, never()).map(any(Cart.class), eq(CartDTO.class));
    }

    /// getCart()
    @Test
    void getCartShouldReturnACartWithoutItems() {
        User user = createUser(1L);

        Cart cart = createCart(user);
        cart.setTotalPrice(BigDecimal.ZERO);

        CartDTO cartDTO = createCartDTO(cart);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(modelMapper.map(cart, CartDTO.class))
                .thenReturn(cartDTO);

        CartDTO result = cartService.getCart();

        assertNotNull(result);
        assertEquals(cart.getCartId() ,result.getCartId());
        assertEquals(cart.getTotalPrice(), result.getTotalPrice());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(modelMapper).map(cart, CartDTO.class);
        verify(modelMapper, never()).map(any(Product.class), eq(CartItemResponseDTO.class));
    }

    @Test
    void getCartShouldReturnACartWithItems() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);

        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        CartDTO cartDTO = createCartDTO(cart);

        CartItemResponseDTO cartItemResponseDTO = createCartItemResponseDTO(product);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(modelMapper.map(cart, CartDTO.class))
                .thenReturn(cartDTO);

        when(modelMapper.map(product, CartItemResponseDTO.class))
                .thenReturn(cartItemResponseDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn("http://localhost/images/" + product.getImage());

        CartDTO result = cartService.getCart();

        assertNotNull(result);
        assertEquals(cart.getCartId(), result.getCartId());
        assertEquals(cart.getTotalPrice(), result.getTotalPrice());

        assertNotNull(result.getCartItems());
        assertEquals(1, result.getCartItems().size());

        CartItemResponseDTO returnedItem = result.getCartItems().getFirst();

        assertEquals(product.getProductId(), returnedItem.getProductId());
        assertEquals(product.getProductName(), returnedItem.getProductName());
        assertEquals(cartItem.getQuantity(), returnedItem.getQuantity());
        assertEquals(product.getPrice(), returnedItem.getPrice());
        assertEquals(product.getDiscount(), returnedItem.getDiscount());
        assertEquals(product.getSpecialPrice(), returnedItem.getSpecialPrice());
        assertEquals("http://localhost/images/" + product.getImage(), returnedItem.getImage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(modelMapper).map(cart, CartDTO.class);
        verify(modelMapper).map(product, CartItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
    }

    @Test
    void getCartShouldThrowApiExceptionWhenCartDoesNotExist() {
        User user = createUser(1L);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(null);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.getCart()
        );

        assertEquals("Cart not yet created!", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(modelMapper, never()).map(any(Cart.class), eq(CartDTO.class));
    }

    /// updateProductQuantityInCart()
    @Test
    void updateProductQuantityInCartShouldUpdateProductQuantityByOne() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);
        cartItem.setQuantity(3);

        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        CartDTO cartDTO = createCartDTO(cart);

        CartItemResponseDTO cartItemResponseDTO = createCartItemResponseDTO(product);

        String op = "add";
        int initialQuantity = cartItem.getQuantity();

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId()))
                .thenReturn(cartItem);

        when(cartRepository.save(cart))
                .thenReturn(cart);

        when(modelMapper.map(cart, CartDTO.class))
                .thenReturn(cartDTO);

        when(modelMapper.map(product, CartItemResponseDTO.class))
                .thenReturn(cartItemResponseDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn("http://localhost/images/" + product.getImage());

        CartDTO result = cartService.updateProductQuantityInCart(1L, op);

        assertNotNull(result);

        CartItemResponseDTO returnedItem = result.getCartItems().getFirst();

        assertEquals(initialQuantity + 1, returnedItem.getQuantity());
        assertEquals(product.getSpecialPrice().multiply(BigDecimal.valueOf(initialQuantity + 1)), cart.getTotalPrice());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId());
        verify(cartRepository).save(cart);
        verify(modelMapper).map(cart, CartDTO.class);
        verify(modelMapper).map(product, CartItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
    }

    @Test
    void updateProductQuantityInCartShouldThrowApiExceptionIfOperationNotAllowed() {
        User user = createUser(1L);

        String op = "xyz";

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.updateProductQuantityInCart(1L, op)
        );

        assertEquals("Invalid operation. Supported operations: add, delete", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository, never()).findCartByUserId(user.getUserId());
    }

    @Test
    void updateProductQuantityInCartShouldThrowApiExceptionIfCartDoesNotExist() {
        User user = createUser(1L);

        String op = "add";

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(null);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.updateProductQuantityInCart(1L, op)
        );

        assertEquals("Cart not yet created!", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductQuantityInCartShouldThrowResourceNotFoundExceptionIfProductDoesNotExist() {
        User user = createUser(1L);

        Cart cart = createCart(user);
        cart.setTotalPrice(BigDecimal.ZERO);

        String op = "add";

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateProductQuantityInCart(1L, op)
        );

        assertEquals("Product not found with productId: 1", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void updateProductQuantityInCartShouldThrowApiExceptionIfCartItemDoesNotExist() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart cart = createCart(user);
        cart.setTotalPrice(BigDecimal.ZERO);

        String op = "add";

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId()))
                .thenReturn(null);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.updateProductQuantityInCart(1L, op)
        );

        assertEquals("Product " + product.getProductName() + " not available in the cart!", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId());
        verify(cartRepository, never()).save(cart);
    }

    @Test
    void updateProductQuantityInCartShouldThrowApiExceptionIfProductStockQuantityIsZero() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);
        product.setQuantity(0);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);

        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        String op = "add";

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId()))
                .thenReturn(cartItem);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.updateProductQuantityInCart(1L, op)
        );

        assertEquals(product.getProductName() + " is out of stock", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId());
        verify(cartRepository, never()).save(cart);
    }

    @Test
    void updateProductQuantityInCartShouldThrowApiExceptionIfProductStockQuantityIsInsufficient() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);
        product.setQuantity(3);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);
        cartItem.setQuantity(3);

        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        String op = "add";

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId()))
                .thenReturn(cartItem);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.updateProductQuantityInCart(1L, op)
        );

        assertEquals("Please, make an order of the " + product.getProductName() +
                " less than or equal to the stock quantity " + product.getQuantity() + ".", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId());
        verify(cartRepository, never()).save(cart);
    }

    @Test
    void updateProductQuantityInCartShouldPreventNegativeQuantity() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);
        cartItem.setQuantity(0);

        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        String op = "delete";

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId()))
                .thenReturn(cartItem);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.updateProductQuantityInCart(1L, op)
        );

        assertEquals("The resulting quantity cannot be negative", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId());
        verify(cartRepository, never()).save(cart);
    }

    @Test
    void updateProductQuantityInCartShouldDeleteProductFromCartIfNewQuantityIsZero() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);
        cartItem.setQuantity(1);

        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        CartDTO cartDTO = createCartDTO(cart);
        cartDTO.setTotalPrice(BigDecimal.ZERO);

        String op = "delete";

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId()))
                .thenReturn(cartItem);

        when(modelMapper.map(cart, CartDTO.class))
                .thenReturn(cartDTO);

        CartDTO result = cartService.updateProductQuantityInCart(1L, op);

        assertNotNull(result);
        assertEquals(0, result.getCartItems().size());
        assertEquals(0, result.getTotalPrice().compareTo(BigDecimal.ZERO));

        assertEquals(0, cart.getCartItems().size());
        assertEquals(0, cart.getTotalPrice().compareTo(BigDecimal.ZERO));

        verify(authUtil, times(2)).loggedInUserId();
        verify(cartRepository, times(2)).findCartByUserId(user.getUserId());
        verify(productRepository).findById(1L);
        verify(cartItemRepository, times(2)).findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId());
        verify(modelMapper).map(cart, CartDTO.class);
        verify(modelMapper, never()).map(product, CartItemResponseDTO.class);
        verify(imageUrlUtil, never()).constructImageUrl(product.getImage());
        verify(cartRepository, never()).save(cart);
    }

    /// deleteProductFromCart()
    @Test
    void deleteProductFromCartShouldDeleteProductFromCart() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);

        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), 1L))
                .thenReturn(cartItem);

        String result = cartService.deleteProductFromCart(1L);

        assertNotNull(result);
        assertEquals("Product " + cartItem.getProduct().getProductName() + " removed from the cart!", result);
        assertEquals(0, cart.getCartItems().size());
        assertTrue(cart.getCartItems().isEmpty());
        assertEquals(0, cart.getTotalPrice().compareTo(BigDecimal.ZERO));

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(cartItemRepository).findCartItemByProductIdAndCartId(cart.getCartId(), product.getProductId());
    }

    @Test
    void deleteProductFromCartShouldThrowApiExceptionWhenCartDoesNotExist() {
        User user = createUser(1L);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(null);

        APIException exception = assertThrows(
                APIException.class,
                () -> cartService.deleteProductFromCart(1L)
        );

        assertEquals("Cart not yet created!", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void deleteProductFromCartShouldThrowResourceNotFoundExceptionWhenCartItemDoesNotExist() {
        User user = createUser(1L);

        Cart cart = createCart(user);
        cart.setTotalPrice(BigDecimal.ZERO);

        when(authUtil.loggedInUserId())
                .thenReturn(user.getUserId());

        when(cartRepository.findCartByUserId(user.getUserId()))
                .thenReturn(cart);

        when(cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), 1L))
                .thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.deleteProductFromCart(1L)
        );

        assertEquals("CartItem not found with productId: 1", exception.getMessage());

        verify(authUtil).loggedInUserId();
        verify(cartRepository).findCartByUserId(user.getUserId());
        verify(cartItemRepository).findCartItemByProductIdAndCartId(cart.getCartId(), 1L);
    }

    /// HELPERS
    private Category createCategory() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Books");
        return category;
    }

    private User createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("Test User");
        user.setEmail("user@gmail.com");
        user.setPassword("password");
        return user;
    }

    private Product createProduct(User owner, Category category) {
        Product product = new Product();

        product.setProductName("Harry Potter 3");
        product.setImage("default.png");
        product.setDescription("Harry Potter and the Prisoner of Azkaban");
        product.setQuantity(10);
        product.setPrice(new BigDecimal("100"));
        product.setDiscount(new BigDecimal("10"));
        product.setSpecialPrice(new BigDecimal("90.00"));
        product.setCategory(category);
        product.setUser(owner);

        return product;
    }

    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setCartId(1L);
        cart.setUser(user);
        return cart;
    }

    private CartItem createCartItem(Product product) {
        CartItem cartItem = new CartItem();

        cartItem.setCartItemId(1L);
        cartItem.setProduct(product);
        cartItem.setQuantity(3);
        cartItem.setDiscount(product.getDiscount());
        cartItem.setProductPrice(product.getSpecialPrice());

        return cartItem;
    }

    private CartDTO createCartDTO(Cart cart) {
        CartDTO cartDTO = new CartDTO();
        cartDTO.setCartId(cart.getCartId());
        cartDTO.setTotalPrice(cart.getTotalPrice());
        return cartDTO;
    }

    private CartItemResponseDTO createCartItemResponseDTO(Product product) {
        CartItemResponseDTO cartItemResponseDTO = new CartItemResponseDTO();

        cartItemResponseDTO.setProductId(product.getProductId());
        cartItemResponseDTO.setProductName(product.getProductName());
        cartItemResponseDTO.setImage("http://localhost/images/" + product.getImage());
        cartItemResponseDTO.setDescription(product.getDescription());
        cartItemResponseDTO.setPrice(product.getPrice());
        cartItemResponseDTO.setDiscount(product.getDiscount());
        cartItemResponseDTO.setSpecialPrice(product.getSpecialPrice());

        return cartItemResponseDTO;
    }

}
