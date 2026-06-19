package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CreateProductRequest;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import com.ecommerce.project.util.ImageUrlUtil;
import com.ecommerce.project.util.PaginationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @InjectMocks
    ProductServiceImpl productService;

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    ModelMapper modelMapper;

    @Mock
    AuthUtil authUtil;

    @Mock
    ImageUrlUtil imageUrlUtil;

    @Mock
    PaginationValidator paginationValidator;

    @Mock
    FileService fileService;

    /// addProduct()
    @Test
    void addProductShouldAddProductSuccessfully() {
        Category category = createCategory();
        User user = createUser(1L);
        CreateProductRequest productRequest = createProductRequest();

        Product product = createProduct(user, category);
        product.setProductName(productRequest.getProductName());
        product.setDescription(productRequest.getDescription());
        product.setQuantity(productRequest.getQuantity());
        product.setPrice(productRequest.getPrice());
        product.setDiscount(productRequest.getDiscount());
        product.setSpecialPrice(new BigDecimal("47.50"));

        Product savedProduct = createProduct(user, category);
        savedProduct.setProductId(1L);

        savedProduct.setProductName(product.getProductName());
        savedProduct.setDescription(product.getDescription());
        savedProduct.setQuantity(product.getQuantity());
        savedProduct.setPrice(product.getPrice());
        savedProduct.setDiscount(product.getDiscount());
        savedProduct.setSpecialPrice(product.getSpecialPrice());

        ProductDTO productDTO = createProductDTO(savedProduct);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(productRepository.existsByProductNameIgnoreCase(productRequest.getProductName()))
                .thenReturn(false);

        when(modelMapper.map(productRequest, Product.class))
                .thenReturn(product);

        when(authUtil.loggedInUser())
                .thenReturn(user);

        when(productRepository.save(product))
                .thenReturn(savedProduct);

        when(modelMapper.map(savedProduct, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(savedProduct.getImage()))
                .thenReturn(productDTO.getImage());

        ProductDTO result = productService.addProduct(category.getCategoryId(), productRequest);

        assertNotNull(result);
        assertEquals(productRequest.getProductName(), result.getProductName());
        assertEquals(1L, result.getProductId());
        assertEquals(new BigDecimal("47.50"), result.getSpecialPrice());
        assertEquals("http://localhost/images/default.png", result.getImage());

        verify(categoryRepository).findById(category.getCategoryId());
        verify(productRepository).existsByProductNameIgnoreCase(productRequest.getProductName());
        verify(modelMapper).map(productRequest, Product.class);
        verify(authUtil).loggedInUser();
        verify(productRepository).save(product);
        verify(modelMapper).map(savedProduct, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(savedProduct.getImage());
    }

    @Test
    void addProductShouldThrowResourceNotFoundExceptionWhenCategoryDoesNotExist() {
        CreateProductRequest productRequest = createProductRequest();

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.addProduct(1L, productRequest)
        );

        assertEquals("Category not found with categoryId: 1", exception.getMessage());
        verify(categoryRepository).findById(1L);
        verify(productRepository, never()).existsByProductNameIgnoreCase(productRequest.getProductName());
    }

    @Test
    void addProductShouldThrowApiExceptionWhenProductAlreadyExists() {
        CreateProductRequest productRequest = createProductRequest();
        Category category = createCategory();

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(productRepository.existsByProductNameIgnoreCase(productRequest.getProductName()))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> productService.addProduct(category.getCategoryId(), productRequest)
        );

        assertEquals("Product with name: " + productRequest.getProductName() + " already exists.", exception.getMessage());

        verify(categoryRepository).findById(1L);
        verify(productRepository).existsByProductNameIgnoreCase(productRequest.getProductName());
        verify(modelMapper, never()).map(productRequest, Product.class);
        verify(productRepository, never()).save(any(Product.class));
    }

    /// getAllProducts()
    @Test
    void getAllProductsShouldReturnAllProducts() {
        Category category = createCategory();
        User user = createUser(1L);

        Product product = createProduct(user, category);
        product.setProductId(1L);

        ProductDTO productDTO = createProductDTO(product);

        List<Product> products = List.of(product);
        Page<Product> productPage = new PageImpl<>(products);

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(productRepository.findAll(
                any(Specification.class), any(Pageable.class)
        )).thenReturn(productPage);

        when(modelMapper.map(product, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn(productDTO.getImage());

        ProductResponse result = productService.getAllProducts(0, 10, "productId", "asc", null, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(productDTO.getProductName(), result.getContent().getFirst().getProductName());
        assertEquals(1L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(modelMapper).map(product, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
    }

    @Test
    void getAllProductsShouldReturnEmptyPageWhenNoProductsExist() {
        Page<Product> emptyPage = Page.empty();

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        ProductResponse result = productService.getAllProducts(0, 10, "productId", "asc", null, null);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        verifyNoInteractions(modelMapper);
        verifyNoInteractions(imageUrlUtil);
    }

    /// getAllProductsForSeller()
    @Test
    void getAllProductsForSellerShouldReturnAllProductsBySeller() {
        Category category = createCategory();
        User seller = createSeller(1L, "Test Seller");

        Product product = createProduct(seller, category);
        product.setProductId(1L);

        ProductDTO productDTO = createProductDTO(product);

        List<Product> products = List.of(product);
        Page<Product> productPage = new PageImpl<>(products);

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(authUtil.loggedInUser()).thenReturn(seller);

        when(productRepository.findByUser(eq(seller), any(Pageable.class)))
                .thenReturn(productPage);

        when(modelMapper.map(product, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn(productDTO.getImage());

        ProductResponse result = productService.getAllProductsForSeller(0, 10, "productId", "asc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(productDTO.getProductName(), result.getContent().getFirst().getProductName());
        assertEquals(1L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(authUtil).loggedInUser();
        verify(productRepository).findByUser(eq(seller), any(Pageable.class));
        verify(modelMapper).map(product, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
    }

    @Test
    void getAllProductsForSellerShouldReturnEmptyPageWhenNoProductExistForThatSeller() {
        User seller = createSeller(1L, "Test Seller");
        Page<Product> emptyPage = Page.empty();

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(authUtil.loggedInUser()).thenReturn(seller);

        when(productRepository.findByUser(eq(seller), any(Pageable.class)))
                .thenReturn(emptyPage);

        ProductResponse result = productService.getAllProductsForSeller(0, 10, "productId", "asc");

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(authUtil).loggedInUser();
        verify(productRepository).findByUser(eq(seller), any(Pageable.class));
        verifyNoInteractions(modelMapper);
        verifyNoInteractions(imageUrlUtil);
    }

    /// searchByCategory()
    @Test
    void searchByCategoryShouldReturnProductsByCategory() {
        Category category = createCategory();
        User user = createUser(1L);

        Product product = createProduct(user, category);
        product.setProductId(1L);

        ProductDTO productDTO = createProductDTO(product);

        List<Product> products = List.of(product);
        Page<Product> productPage = new PageImpl<>(products);

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(categoryRepository.findById(category.getCategoryId()))
                .thenReturn(Optional.of(category));

        when(productRepository.findByCategory(eq(category), any(Pageable.class)))
                .thenReturn(productPage);

        when(modelMapper.map(product, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn(productDTO.getImage());

        ProductResponse result = productService.searchByCategory(1L, 0, 10, "productId", "asc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(productDTO.getProductName(), result.getContent().getFirst().getProductName());
        assertEquals(1L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(categoryRepository).findById(1L);
        verify(productRepository).findByCategory(eq(category), any(Pageable.class));
        verify(modelMapper).map(product, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
    }

    @Test
    void searchByCategoryShouldThrowResourceNotFoundExceptionWhenCategoryDoesNotExist() {
        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.searchByCategory(1L, 0, 10, "productId", "asc")
        );

        assertEquals("Category not found with categoryId: 1", exception.getMessage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(categoryRepository).findById(1L);
        verify(productRepository, never()).findByCategory(any(), any());
    }

    @Test
    void searchByCategoryShouldReturnEmptyPageWhenNoProductExistForThatCategory() {
        Category category = createCategory();
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(categoryRepository.findById(category.getCategoryId()))
                .thenReturn(Optional.of(category));

        when(productRepository.findByCategory(eq(category), any(Pageable.class)))
                .thenReturn(emptyPage);

        ProductResponse result = productService.searchByCategory(1L, 0, 10, "productId", "asc");

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(categoryRepository).findById(1L);
        verify(productRepository).findByCategory(eq(category), any(Pageable.class));
        verifyNoInteractions(modelMapper);
        verifyNoInteractions(imageUrlUtil);
    }

    /// searchByKeyword()
    @Test
    void searchByKeywordShouldReturnProductsByKeyword() {
        Category category = createCategory();
        User user = createUser(1L);

        Product product = createProduct(user, category);
        product.setProductId(1L);

        ProductDTO productDTO = createProductDTO(product);

        String keyword = "har";

        List<Product> products = List.of(product);
        Page<Product> productPage = new PageImpl<>(products);

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(productRepository.findByProductNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                .thenReturn(productPage);

        when(modelMapper.map(product, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(product.getImage()))
                .thenReturn(productDTO.getImage());

        ProductResponse result = productService.searchProductByKeyword(keyword, 0, 10, "productId", "asc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(productDTO.getProductName(), result.getContent().getFirst().getProductName());
        assertEquals(1L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(productRepository).findByProductNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
        verify(modelMapper).map(product, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
    }

    @Test
    void searchByKeywordShouldReturnEmptyPageWhenNoProductExistForThatKeyword() {
        String keyword = "har";

        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(productRepository.findByProductNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                .thenReturn(emptyPage);

        ProductResponse result = productService.searchProductByKeyword(keyword, 0, 10, "productId", "asc");

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("productId"), eq("asc"), anyList());
        verify(productRepository).findByProductNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
        verifyNoInteractions(modelMapper);
        verifyNoInteractions(imageUrlUtil);
    }

    /// updateProduct()
    @Test
    void updateProductShouldUpdateProduct() {
        Category category = createCategory();
        User user = createUser(1L);

        CreateProductRequest productRequest = updateProductRequest();

        Product productFromDB = createProduct(user, category);
        productFromDB.setProductId(1L);

        Cart cart = createCart(user);

        CartItem cartItem = createCartItem(productFromDB);
        cartItem.setCart(cart);

        List<CartItem> cartItems = List.of(cartItem);

        cart.setCartItems(cartItems);
        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

        productFromDB.setCartItems(cartItems);

        ProductDTO updatedProductDTO = createProductDTO(productFromDB);
        updatedProductDTO.setProductName(productRequest.getProductName());
        updatedProductDTO.setDescription(productRequest.getDescription());
        updatedProductDTO.setQuantity(productRequest.getQuantity());
        updatedProductDTO.setPrice(productRequest.getPrice());
        updatedProductDTO.setDiscount(productRequest.getDiscount());

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        when(productRepository.existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(), 1L))
                .thenReturn(false);

        when(cartItemRepository.findAllByProductId(productFromDB.getProductId()))
                .thenReturn(cartItems);

        when(modelMapper.map(productFromDB, ProductDTO.class))
                .thenReturn(updatedProductDTO);

        when(imageUrlUtil.constructImageUrl(productFromDB.getImage()))
                .thenReturn("http://localhost/images/default.png");

        ProductDTO result = productService.updateProduct(productRequest, 1L);

        assertNotNull(result);
        assertEquals(productRequest.getProductName(), result.getProductName());
        assertEquals(productRequest.getDescription(), result.getDescription());
        assertEquals(productRequest.getQuantity(), result.getQuantity());
        assertEquals(productRequest.getPrice(), result.getPrice());
        assertEquals(productRequest.getDiscount(), result.getDiscount());

        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice(), productFromDB.getPrice());
        assertEquals(productRequest.getDiscount(), productFromDB.getDiscount());
        assertEquals(0, new BigDecimal("54").compareTo(productFromDB.getSpecialPrice()));

        assertEquals(0, new BigDecimal("10").compareTo(cartItem.getDiscount()));
        assertEquals(0, new BigDecimal("54").compareTo(cartItem.getProductPrice()));
        assertEquals(0, cartItem.getProductPrice().multiply(new BigDecimal(cartItem.getQuantity())).compareTo(cart.getTotalPrice()));

        verify(productRepository).findById(1L);
        verify(productRepository).existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(), 1L);
        verify(cartItemRepository).findAllByProductId(productFromDB.getProductId());
        verify(modelMapper).map(productFromDB, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(productFromDB.getImage());
    }

    @Test
    void updateProductShouldThrowResourceNotFoundExceptionWhenProductDoesNotExist() {
        CreateProductRequest productRequest = updateProductRequest();

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProduct(productRequest, 1L)
        );

        assertEquals("Product not found with productId: 1", exception.getMessage());

        verify(productRepository).findById(1L);
        verify(productRepository, never()).existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(),1L);
    }

    @Test
    void updateProductShouldThrowApiExceptionWhenAnotherProductHasSameName() {
        Category category = createCategory();
        User user = createUser(1L);

        CreateProductRequest productRequest = updateProductRequest();

        Product productFromDB = createProduct(user, category);
        productFromDB.setProductId(1L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        when(productRepository.existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(), 1L))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> productService.updateProduct(productRequest, 1L)
        );

        assertEquals("Product with name: " + productRequest.getProductName() + " already exists.", exception.getMessage());

        verify(productRepository).findById(1L);
        verify(productRepository).existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(),1L);
        verify(cartItemRepository, never()).findAllByProductId(productFromDB.getProductId());
    }

    /// updateProductSeller()
    @Test
    void updateProductSellerShouldUpdateProductOwnedByTheSeller() {
        Category category = createCategory();
        User seller = createSeller(1L, "Test Seller");

        CreateProductRequest productRequest = updateProductRequest();

        Product productFromDB = createProduct(seller, category);
        productFromDB.setProductId(1L);

        Cart cart = createCart(seller);

        CartItem cartItem = createCartItem(productFromDB);
        cartItem.setCart(cart);

        List<CartItem> cartItems = List.of(cartItem);

        cart.setCartItems(cartItems);
        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

        productFromDB.setCartItems(cartItems);

        ProductDTO updatedProductDTO = createProductDTO(productFromDB);
        updatedProductDTO.setProductName(productRequest.getProductName());
        updatedProductDTO.setDescription(productRequest.getDescription());
        updatedProductDTO.setQuantity(productRequest.getQuantity());
        updatedProductDTO.setPrice(productRequest.getPrice());
        updatedProductDTO.setDiscount(productRequest.getDiscount());

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        when(productRepository.existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(), 1L))
                .thenReturn(false);

        when(cartItemRepository.findAllByProductId(productFromDB.getProductId()))
                .thenReturn(cartItems);

        when(modelMapper.map(productFromDB, ProductDTO.class))
                .thenReturn(updatedProductDTO);

        when(imageUrlUtil.constructImageUrl(productFromDB.getImage()))
                .thenReturn("http://localhost/images/default.png");

        ProductDTO result = productService.updateProductSeller(productRequest, 1L);

        assertNotNull(result);
        assertEquals(productRequest.getProductName(), result.getProductName());
        assertEquals(productRequest.getDescription(), result.getDescription());
        assertEquals(productRequest.getQuantity(), result.getQuantity());
        assertEquals(productRequest.getPrice(), result.getPrice());
        assertEquals(productRequest.getDiscount(), result.getDiscount());

        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice(), productFromDB.getPrice());
        assertEquals(productRequest.getDiscount(), productFromDB.getDiscount());
        assertEquals(0, new BigDecimal("54").compareTo(productFromDB.getSpecialPrice()));

        assertEquals(0, new BigDecimal("10").compareTo(cartItem.getDiscount()));
        assertEquals(0, new BigDecimal("54").compareTo(cartItem.getProductPrice()));
        assertEquals(0, cartItem.getProductPrice().multiply(new BigDecimal(cartItem.getQuantity())).compareTo(cart.getTotalPrice()));

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(productRepository).existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(), 1L);
        verify(cartItemRepository).findAllByProductId(productFromDB.getProductId());
        verify(modelMapper).map(productFromDB, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(productFromDB.getImage());
    }

    @Test
    void updateProductSellerShouldThrowResourceNotFoundExceptionWhenProductDoesNotExist() {
        User seller = createSeller(1L, "Test Seller");

        CreateProductRequest productRequest = updateProductRequest();

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProductSeller(productRequest, 1L)
        );

        assertEquals("Product not found with productId: 1", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(productRepository, never()).existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(),1L);
    }

    @Test
    void updateProductSellerShouldThrowApiExceptionWhenSellerDoesNotOwnProduct() {
        Category category = createCategory();

        User seller1 = createSeller(1L, "Test Seller 1");
        User seller2 = createSeller(2L, "Test Seller 2");

        CreateProductRequest productRequest = updateProductRequest();

        Product productFromDB = createProduct(seller1, category);
        productFromDB.setProductId(1L);

        when(authUtil.loggedInUser())
                .thenReturn(seller2);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        APIException exception = assertThrows(
                APIException.class,
                () -> productService.updateProductSeller(productRequest, 1L)
        );

        assertEquals("Product cannot be updated as it does not belong to this seller!", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(productRepository, never()).existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(),1L);
    }

    @Test
    void updateProductSellerShouldThrowApiExceptionWhenAnotherProductHasSameName() {
        Category category = createCategory();
        User seller = createSeller(1L, "Test Seller");

        CreateProductRequest productRequest = updateProductRequest();

        Product productFromDB = createProduct(seller, category);
        productFromDB.setProductId(1L);

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        when(productRepository.existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(), 1L))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> productService.updateProductSeller(productRequest, 1L)
        );

        assertEquals("Product with name: " + productRequest.getProductName() + " already exists.", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(productRepository).existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(),1L);
        verify(cartItemRepository, never()).findAllByProductId(productFromDB.getProductId());
    }

    /// deleteProduct()
    @Test
    void deleteProductShouldDeleteProduct() {
        Category category = createCategory();
        User user = createUser(1L);

        Product productFromDB = createProduct(user, category);
        productFromDB.setProductId(1L);

        Cart cart = createCart(user);
        CartItem cartItem = createCartItem(productFromDB);
        cartItem.setCart(cart);

        List<CartItem> cartItems = List.of(cartItem);

        cart.setCartItems(cartItems);
        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

        productFromDB.setCartItems(cartItems);

        ProductDTO productDTO = createProductDTO(productFromDB);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        when(cartItemRepository.findAllByProductId(1L))
                .thenReturn(cartItems);

        doNothing().when(cartItemRepository).deleteAll(cartItems);

        doNothing().when(productRepository).delete(productFromDB);

        when(modelMapper.map(productFromDB, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(productFromDB.getImage()))
                .thenReturn("http://localhost/images/default.png");

        ProductDTO result = productService.deleteProduct(1L);

        assertNotNull(result);
        assertEquals(productFromDB.getProductName(), result.getProductName());
        assertEquals(productFromDB.getDescription(), result.getDescription());
        assertEquals(productFromDB.getQuantity(), result.getQuantity());
        assertEquals(productFromDB.getPrice(), result.getPrice());
        assertEquals(productFromDB.getDiscount(), result.getDiscount());
        assertEquals(productFromDB.getSpecialPrice(), result.getSpecialPrice());
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getTotalPrice()));

        verify(productRepository).findById(1L);
        verify(cartItemRepository).findAllByProductId(1L);
        verify(cartItemRepository).deleteAll(cartItems);
        verify(productRepository).delete(productFromDB);
        verify(modelMapper).map(productFromDB, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(productFromDB.getImage());
    }

    @Test
    void deleteProductShouldThrowResourceNotFoundExceptionWhenProductDoesNotExist() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.deleteProduct(1L)
        );

        assertEquals("Product not found with productId: 1", exception.getMessage());

        verify(productRepository).findById(1L);
        verify(cartItemRepository, never()).findAllByProductId(1L);
    }

    /// deleteProductSeller()
    @Test
    void deleteProductSellerShouldDeleteProductOwnedByTheSeller() {
        Category category = createCategory();
        User seller = createSeller(1L, "Test Seller");

        Product productFromDB = createProduct(seller, category);
        productFromDB.setProductId(1L);

        Cart cart = createCart(seller);

        CartItem cartItem = createCartItem(productFromDB);
        cartItem.setCart(cart);

        List<CartItem> cartItems = List.of(cartItem);

        cart.setCartItems(cartItems);
        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

        productFromDB.setCartItems(cartItems);

        ProductDTO productDTO = createProductDTO(productFromDB);

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        when(cartItemRepository.findAllByProductId(1L))
                .thenReturn(cartItems);

        doNothing().when(cartItemRepository).deleteAll(cartItems);

        doNothing().when(productRepository).delete(productFromDB);

        when(modelMapper.map(productFromDB, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(productFromDB.getImage()))
                .thenReturn("http://localhost/images/default.png");

        ProductDTO result = productService.deleteProductSeller(1L);

        assertNotNull(result);
        assertEquals(productFromDB.getProductName(), result.getProductName());
        assertEquals(productFromDB.getDescription(), result.getDescription());
        assertEquals(productFromDB.getQuantity(), result.getQuantity());
        assertEquals(productFromDB.getPrice(), result.getPrice());
        assertEquals(productFromDB.getDiscount(), result.getDiscount());
        assertEquals(productFromDB.getSpecialPrice(), result.getSpecialPrice());
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getTotalPrice()));

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(cartItemRepository).findAllByProductId(1L);
        verify(cartItemRepository).deleteAll(cartItems);
        verify(productRepository).delete(productFromDB);
        verify(modelMapper).map(productFromDB, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(productFromDB.getImage());
    }

    @Test
    void deleteProductSellerShouldThrowResourceNotFoundExceptionWhenProductDoesNotExist() {
        User seller = createSeller(1L, "Test Seller");

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.deleteProductSeller(1L)
        );

        assertEquals("Product not found with productId: 1", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(cartItemRepository, never()).findAllByProductId(1L);
        verify(productRepository, never()).delete(any(Product.class));
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Test
    void deleteProductSellerShouldThrowApiExceptionWhenSellerDoesNotOwnProduct() {
        Category category = createCategory();

        User seller1 = createSeller(1L, "Test Seller 1");
        User seller2 = createSeller(2L, "Test Seller 2");

        Product productFromDB = createProduct(seller1, category);
        productFromDB.setProductId(1L);

        when(authUtil.loggedInUser())
                .thenReturn(seller2);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        APIException exception = assertThrows(
                APIException.class,
                () -> productService.deleteProductSeller(1L)
        );

        assertEquals("Product cannot be deleted as it does not belong to this seller!", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(cartItemRepository, never()).findAllByProductId(1L);
        verify(productRepository, never()).delete(any(Product.class));
        verify(cartItemRepository, never()).deleteAll(any());
    }

    /// updateProductImage()
    @Test
    void updateProductImageShouldUpdateProductImage() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "dummy image data".getBytes()
        );

        String path = "images/";
        setField(productService, "path", "images/");

        String fileName = "00c3d323-b130-452d-a7e3-d65bebef3326.png";

        Category category = createCategory();
        User user = createUser(1L);

        Product productFromDB = createProduct(user, category);
        productFromDB.setProductId(1L);

        Product updatedProduct = createProduct(user, category);
        updatedProduct.setProductId(1L);
        updatedProduct.setImage(fileName);

        ProductDTO productDTO = createProductDTO(updatedProduct);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        when(fileService.uploadImage(path, image))
                .thenReturn(fileName);

        when(productRepository.save(productFromDB))
                .thenReturn(updatedProduct);

        when(modelMapper.map(updatedProduct, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(fileName))
                .thenReturn("http://localhost/images/" + fileName);

        ProductDTO result = productService.updateProductImage(1L, image);

        assertNotNull(result);
        assertEquals(fileName, productFromDB.getImage());
        assertEquals("http://localhost/images/" + fileName, result.getImage());

        verify(productRepository).findById(1L);
        verify(fileService).uploadImage(path, image);
        verify(productRepository).save(productFromDB);
        verify(modelMapper).map(updatedProduct, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(fileName);
    }

    @Test
    void updateProductImageShouldThrowApiExceptionWhenImageIsEmpty() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "".getBytes()
        );

        APIException exception = assertThrows(
                APIException.class,
                ()-> productService.updateProductImage(1L, image)
        );

        assertEquals("Image cannot be empty", exception.getMessage());

        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductImageShouldThrowApiExceptionWhenContentTypeIsNull() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                null,
                "dummy image data".getBytes()
        );

        APIException exception = assertThrows(
                APIException.class,
                ()-> productService.updateProductImage(1L, image)
        );

        assertEquals("Invalid image type. Valid image types: PNG, JPEG, WEBP", exception.getMessage());

        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductImageShouldThrowApiExceptionWhenContentTypeIsInvalid() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.svg",
                "image/svg",
                "dummy image data".getBytes()
        );

        APIException exception = assertThrows(
                APIException.class,
                ()-> productService.updateProductImage(1L, image)
        );

        assertEquals("Invalid image type. Valid image types: PNG, JPEG, WEBP", exception.getMessage());

        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductImageShouldThrowApiExceptionWhenImageSizeExceeds5MB() {
        byte[] largeImage = new byte[5 * 1024 * 1024 + 1];

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                largeImage
        );

        APIException exception = assertThrows(
                APIException.class,
                () -> productService.updateProductImage(1L, image)
        );

        assertEquals("Image size exceeds 5MB", exception.getMessage());

        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductImageShouldThrowResourceNotFoundExceptionWhenProductDoesNotExist() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "dummy image data".getBytes()
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProductImage(1L, image)
        );

        assertEquals("Product not found with productId: 1", exception.getMessage());

        verify(productRepository).findById(1L);
        verify(fileService, never()).uploadImage(anyString(), any());
    }

    /// updateProductImageSeller()
    @Test
    void updateProductImageSellerShouldUpdateProductImageOwnedByTheSeller() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "dummy image data".getBytes()
        );

        String path = "images/";
        setField(productService, "path", "images/");

        String fileName = "00c3d323-b130-452d-a7e3-d65bebef3326.png";

        Category category = createCategory();

        User seller = createSeller(1L, "Test Seller");

        Product productFromDB = createProduct(seller, category);
        productFromDB.setProductId(1L);

        Product updatedProduct = createProduct(seller, category);
        updatedProduct.setProductId(1L);
        updatedProduct.setImage(fileName);

        ProductDTO productDTO = createProductDTO(updatedProduct);

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        when(fileService.uploadImage(path, image))
                .thenReturn(fileName);

        when(productRepository.save(productFromDB))
                .thenReturn(updatedProduct);

        when(modelMapper.map(updatedProduct, ProductDTO.class))
                .thenReturn(productDTO);

        when(imageUrlUtil.constructImageUrl(fileName))
                .thenReturn("http://localhost/images/" + fileName);

        ProductDTO result = productService.updateProductImageSeller(1L, image);

        assertNotNull(result);
        assertEquals(fileName, productFromDB.getImage());
        assertEquals("http://localhost/images/" + fileName, result.getImage());

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(fileService).uploadImage(path, image);
        verify(productRepository).save(productFromDB);
        verify(modelMapper).map(updatedProduct, ProductDTO.class);
        verify(imageUrlUtil).constructImageUrl(fileName);
    }

    @Test
    void updateProductImageSellerShouldThrowApiExceptionWhenImageIsEmpty() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "".getBytes()
        );

        User seller = createSeller(1L, "Test Seller");

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        APIException exception = assertThrows(
                APIException.class,
                ()-> productService.updateProductImageSeller(1L, image)
        );

        assertEquals("Image cannot be empty", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductImageSellerShouldThrowApiExceptionWhenContentTypeIsNull() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                null,
                "dummy image data".getBytes()
        );

        User seller = createSeller(1L, "Test Seller");

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        APIException exception = assertThrows(
                APIException.class,
                ()-> productService.updateProductImageSeller(1L, image)
        );

        assertEquals("Invalid image type. Valid image types: PNG, JPEG, WEBP", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductImageSellerShouldThrowApiExceptionWhenContentTypeIsInvalid() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.svg",
                "image/svg",
                "dummy image data".getBytes()
        );

        User seller = createSeller(1L, "Test Seller");

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        APIException exception = assertThrows(
                APIException.class,
                ()-> productService.updateProductImageSeller(1L, image)
        );

        assertEquals("Invalid image type. Valid image types: PNG, JPEG, WEBP", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductImageSellerShouldThrowApiExceptionWhenImageSizeExceeds5MB() {
        byte[] largeImage = new byte[5 * 1024 * 1024 + 1];

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                largeImage
        );

        User seller = createSeller(1L, "Test Seller");

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        APIException exception = assertThrows(
                APIException.class,
                () -> productService.updateProductImageSeller(1L, image)
        );

        assertEquals("Image size exceeds 5MB", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository, never()).findById(1L);
    }

    @Test
    void updateProductImageSellerShouldThrowResourceNotFoundExceptionWhenProductDoesNotExist() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "dummy image data".getBytes()
        );

        User seller = createSeller(1L, "Test Seller");

        when(authUtil.loggedInUser())
                .thenReturn(seller);

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProductImageSeller(1L, image)
        );

        assertEquals("Product not found with productId: 1", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(fileService, never()).uploadImage(anyString(), any());
    }

    @Test
    void updateProductImageSellerShouldThrowApiExceptionWhenSellerDoesNotOwnProduct() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "dummy image data".getBytes()
        );

        User seller1 = createSeller(1L, "Test Seller 1");
        User seller2 = createSeller(2L, "Test Seller 2");

        Category category = createCategory();

        Product productFromDB = createProduct(seller1, category);
        productFromDB.setProductId(1L);

        when(authUtil.loggedInUser())
                .thenReturn(seller2);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productFromDB));

        APIException exception = assertThrows(
                APIException.class,
                () -> productService.updateProductImageSeller(1L, image)
        );

        assertEquals("Product image cannot be updated as it does not belong to this seller!", exception.getMessage());

        verify(authUtil).loggedInUser();
        verify(productRepository).findById(1L);
        verify(fileService, never()).uploadImage(anyString(), any());
    }

    /// HELPERS
    /// Category
    private Category createCategory() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Books");
        return category;
    }

    /// User
    private User createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("Test User");
        user.setEmail("user@gmail.com");
        user.setPassword("password");
        return user;
    }

    /// Seller
    private User createSeller(Long id, String name) {
        User seller = new User();
        seller.setUserId(id);
        seller.setUserName(name);
        seller.setEmail(name.toLowerCase().replaceAll("\\s", "") + "@gmail.com");
        seller.setPassword("password");
        return seller;
    }

    /// Product Unsaved
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

    private ProductDTO createProductDTO(Product product) {
        ProductDTO dto = new ProductDTO();

        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setImage("http://localhost/images/" + product.getImage());
        dto.setDescription(product.getDescription());
        dto.setQuantity(product.getQuantity());
        dto.setPrice(product.getPrice());
        dto.setDiscount(product.getDiscount());
        dto.setSpecialPrice(product.getSpecialPrice());

        return dto;
    }

    private CreateProductRequest createProductRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setProductName("Harry Potter 3");
        request.setDescription("Harry Potter and the Prisoner of Azkaban");
        request.setQuantity(5);
        request.setPrice(new BigDecimal("50"));
        request.setDiscount(new BigDecimal("5"));
        return request;
    }

    private CreateProductRequest updateProductRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setProductName("Harry Potter 6");
        request.setDescription("Harry Potter and the Half Blood Prince");
        request.setQuantity(15);
        request.setPrice(new BigDecimal("60"));
        request.setDiscount(new BigDecimal("10"));
        return request;
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
}
